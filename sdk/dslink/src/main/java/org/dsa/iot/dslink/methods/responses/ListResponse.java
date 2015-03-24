package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.StreamState;
import org.dsa.iot.dslink.util.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * @author Samuel Grenier
 */
public class ListResponse implements Response {

    private final DSLink link;
    private final SubscriptionManager manager;
    private final int rid;
    private final Node node;

    public ListResponse(DSLink link, SubscriptionManager manager,
                        int rid, Node node) {
        if (link == null)
            throw new NullPointerException("link");
        else if (manager == null)
            throw new NullPointerException("manager");
        else if (rid <= 0)
            throw new IllegalArgumentException("rid <= 0");
        else if (node == null)
            throw new NullPointerException("node");
        this.link = link;
        this.manager = manager;
        this.rid = rid;
        this.node = node;
    }

    /**
     * @return Node the response corresponds to.
     */
    public Node getNode() {
        return node;
    }

    @Override
    public int getRid() {
        return rid;
    }

    @Override
    public void populate(JsonObject in) {
        JsonArray updates = in.getArray("updates");
        if (updates != null) {
            for (Object obj : updates) {
                update((JsonArray) obj);
            }
        }
    }

    private void update(JsonArray in) {
        String name = in.get(0);
        Object v = in.get(1);

        char start = name.charAt(0);
        if (start == '$') {
            name = name.substring(1);
            if ("is".equals(name)) {
                node.setProfile((String) v);
            } else if ("mixin".equals(name)) {
                node.setMixins((String) v);
            } else if ("interface".equals(name)) {
                node.setInterfaces((String) v);
            } else if ("invokable".equals(name)) {
                Permission perm = Permission.toEnum((String) v);
                node.setAction(new Action("", perm, new Handler<ActionResult>() {
                    @Override
                    public void handle(ActionResult event) {
                        throw new UnsupportedOperationException();
                    }
                }));
            } else {
                node.setConfig(name, ValueUtils.toValue(v));
            }
        } else if (start == '@') {
            name = name.substring(1);
            node.setAttribute(name, ValueUtils.toValue(v));
        } else {
            // Child node
            @SuppressWarnings("ConstantConditions")
            JsonObject childData = (JsonObject) v;
            Node child = node.getChild(name);

            String change = childData.getString("change");
            if (change != null && "remove".equals(change)) {
                if (child != null) {
                    node.removeChild(child.getName());
                }
                return;
            }

            String is = childData.getString("$is");
            if (child == null)
                child = node.createChild(name, is);

            String mixin = childData.getString("$mixin");
            if (mixin != null) {
                child.setMixins(mixin);
            }

            String _interface = childData.getString("$interface");
            if (_interface != null) {
                child.setInterfaces(_interface);
            }

            String displayName = childData.getString("$name");
            if (displayName != null) {
                child.setDisplayName(displayName);
            }

            String type = childData.getString("$type");
            if (type != null) {
                child.setValue(ValueUtils.fromType(type));
            }

            String invokable = childData.getString("$invokable");
            if (invokable != null) {
                Permission perm = Permission.toEnum(invokable);
                child.setAction(new Action("", perm, new Handler<ActionResult>() {
                    @Override
                    public void handle(ActionResult event) {
                        throw new UnsupportedOperationException();
                    }
                }));
            }
        }
    }

    @Override
    public JsonObject getJsonResponse(JsonObject in) {
        JsonObject out = new JsonObject();
        out.putNumber("rid", getRid());
        out.putString("stream", StreamState.OPEN.getJsonName());

        JsonArray updates = new JsonArray();
        {
            // Special configurations
            String profile = node.getProfile();
            if (profile != null) {
                JsonArray update = new JsonArray();
                update.addString("$is");
                update.addString(profile);
                updates.addArray(update);
            } else {
                String err = "Profile not set on node: "
                            + node.getPath();
                throw new RuntimeException(err);
            }

            Set<String> mixins = node.getMixins();
            if (mixins != null && mixins.size() > 0) {
                JsonArray update = new JsonArray();
                update.addString("$mixin");
                update.addString(StringUtils.join(mixins, "|"));
                updates.addArray(update);
            }

            Set<String> interfaces = node.getInterfaces();
            if (interfaces != null && interfaces.size() > 0) {
                JsonArray update = new JsonArray();
                update.addString("$interface");
                update.addString(StringUtils.join(interfaces, "|"));
                updates.addArray(update);
            }

            Action action = node.getAction();
            if (action != null) {
                JsonArray update = new JsonArray();
                update.addString("$invokable");
                update.addString(action.getPermission().getJsonName());
                updates.addArray(update);
            }

            // Attributes and configurations
            add("$", updates, node.getConfigurations());
            add("@", updates, node.getAttributes());

            // Children
            Map<String, Node> children = node.getChildren();
            if (children != null) {
                for (Node child : children.values()) {
                    JsonArray update = getChildUpdate(child, false);
                    updates.addArray(update);
                }
            }
        }
        out.putArray("updates", updates);

        manager.addPathSub(node, this);
        return out;
    }

    public void childUpdate(Node child, boolean removed) {
        if (removed) {
            manager.removePathSub(child);
        }

        JsonArray updates = new JsonArray();
        updates.addArray(getChildUpdate(child, removed));

        JsonObject resp = new JsonObject();
        resp.putNumber("rid", getRid());
        resp.putString("stream", StreamState.OPEN.getJsonName());
        resp.putArray("updates", updates);

        JsonArray responses = new JsonArray();
        responses.addObject(resp);

        JsonObject top = new JsonObject();
        top.putArray("responses", responses);
        link.getClient().write(top);
    }

    @Override
    public JsonObject getCloseResponse() {
        manager.removePathSub(node);

        JsonObject resp = new JsonObject();
        resp.putNumber("rid", getRid());
        resp.putString("stream", StreamState.CLOSED.getJsonName());

        JsonArray responses = new JsonArray();
        responses.addObject(resp);

        JsonObject top = new JsonObject();
        top.putArray("responses", responses);
        return top;
    }

    /**
     * @param prefix Prefix to use (whether its an attribute or config)
     * @param out Updates array
     * @param vals Values to iterate and add to the updates array
     */
    private void add(String prefix, JsonArray out, Map<String, Value> vals) {
        if (vals == null) {
            return;
        }
        for (Map.Entry<String, Value> entry : vals.entrySet()) {
            JsonArray update = new JsonArray();
            update.addString(prefix + entry.getKey());
            ValueUtils.toJson(update, entry.getValue());
            out.addArray(update);
        }
    }

    private JsonArray getChildUpdate(Node child, boolean removed) {
        JsonArray update = new JsonArray();
        update.addString(child.getName());

        JsonObject childData = new JsonObject();
        {
            String displayName = child.getDisplayName();
            if (displayName != null) {
                childData.putString("$name", displayName);
            }

            String profile = child.getProfile();
            if (profile == null) {
                String err = "Profile not set on node: "
                        + child.getPath();
                throw new RuntimeException(err);
            }
            childData.putString("$is", profile);

            Action action = child.getAction();
            if (action != null) {
                action.toJson(childData);
            }

            Set<String> mixins = child.getMixins();
            if (mixins != null) {
                String mixin = StringUtils.join(mixins, "|");
                childData.putString("$mixin", mixin);
            }

            Set<String> interfaces = child.getInterfaces();
            if (interfaces != null) {
                String _interface = StringUtils.join(mixins, "|");
                childData.putString("$interface", _interface);
            }

            Value value = child.getValue();
            if (value != null) {
                String type = value.getType().toJsonString();
                childData.putString("$type", type);
            }

            if (removed) {
                childData.putString("$changed", "remove");
            }
        }
        update.addObject(childData);
        return update;
    }
}
