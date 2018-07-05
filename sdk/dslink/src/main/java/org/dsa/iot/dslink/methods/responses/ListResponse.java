package org.dsa.iot.dslink.methods.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.NodeListener;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.EditorType;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.ResultType;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.TimeUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ListResponse extends Response {

    private final DSLink link;
    private final SubscriptionManager manager;
    private final int rid;
    private final Node node;
    private final String path;

    private final Map<Node, Boolean> updates = new HashMap<>();

    public ListResponse(DSLink link, SubscriptionManager manager,
                        int rid, Node node, String path) {
        if (link == null) {
            throw new NullPointerException("link");
        } else if (manager == null) {
            throw new NullPointerException("manager");
        } else if (rid <= 0) {
            throw new IllegalArgumentException("rid <= 0");
        }
        this.link = link;
        this.manager = manager;
        this.rid = rid;
        this.node = node;
        this.path = path;
    }

    public void childUpdate(Node child, boolean removed) {
        if (removed) {
            manager.removePathSub(child);
        }

        JsonArray updates = new JsonArray();
        updates.add(getChildUpdate(child, removed));

        JsonObject resp = new JsonObject();
        resp.put("rid", getRid());
        resp.put("stream", StreamState.OPEN.getJsonName());
        resp.put("updates", updates);
        link.getWriter().writeResponse(resp);
    }

    @Override
    public JsonObject getCloseResponse() {
        manager.removePathSub(node);
        if (node != null) {
            NodeListener listener = node.getListener();
            if (listener != null) {
                node.getListener().postListClosed();
            }
        }
        JsonObject resp = new JsonObject();
        resp.put("rid", getRid());
        resp.put("stream", StreamState.CLOSED.getJsonName());
        return resp;
    }

    @Override
    public JsonObject getJsonResponse(JsonObject in) {
        JsonObject out = new JsonObject();
        out.put("rid", getRid());
        out.put("stream", StreamState.OPEN.getJsonName());

        JsonArray updates = new JsonArray();
        if (node == null) {
            JsonArray update = new JsonArray();
            update.add("$is").add("node");
            updates.add(update);
            update = new JsonArray();
            update.add("$disconnectedTs")
                  .add(TimeUtils.encode(System.currentTimeMillis(), true).toString());
            updates.add(update);
        } else {
            // Special configurations
            String profile = node.getProfile();
            if (profile != null) {
                JsonArray update = new JsonArray();
                update.add("$is");
                update.add(profile);
                updates.add(update);
            } else {
                String err = "Profile not set on node: "
                        + node.getPath();
                throw new RuntimeException(err);
            }

            String name = node.getDisplayName();
            if (name != null) {
                JsonArray update = new JsonArray();
                update.add("$name");
                update.add(name);
                updates.add(update);
            }

            Set<String> interfaces = node.getInterfaces();
            if (interfaces != null && interfaces.size() > 0) {
                JsonArray update = new JsonArray();
                update.add("$interface");
                update.add(StringUtils.join(interfaces, "|"));
                updates.add(update);
            }

            ValueType type = node.getValueType();
            if (type != null) {
                JsonArray update = new JsonArray();
                update.add("$type");
                update.add(type.toJsonString());
                updates.add(update);
            }

            char[] password = node.getPassword();
            if (password != null) {
                JsonArray update = new JsonArray();
                update.add("$$password");
                update.add(null);
                updates.add(update);
            }

            Writable writable = node.getWritable();
            if (!(writable == null || writable == Writable.NEVER)) {
                JsonArray update = new JsonArray();
                update.add("$writable");
                update.add(writable.toJsonName());
                updates.add(update);
            }

            // Action
            Action action = node.getAction();
            if (action != null
                    && action.hasPermission()) {
                JsonArray update = new JsonArray();
                update.add("$invokable");
                update.add(action.getPermission().getJsonName());
                updates.add(update);

                if (!action.isHidden()) {
                    update = new JsonArray();
                    update.add("$params");
                    update.add(action.getParams());
                    updates.add(update);

                    update = new JsonArray();
                    update.add("$columns");
                    update.add(action.getColumns());
                    updates.add(update);

                    update = new JsonArray();
                    update.add("$result");
                    update.add(action.getResultType().getJsonName());
                    updates.add(update);
                }
            }

            // Attributes and configurations
            add("$$", updates, node.getRoConfigurations());
            add("$", updates, node.getConfigurations());
            add("@", updates, node.getAttributes());

            // Whether this node "has children"
            Boolean hasChildren = node.getHasChildren();
            if (hasChildren != null) {
                JsonArray update = new JsonArray();
                update.add("$hasChildren");
                update.add(hasChildren);
                updates.add(update);
            }

            // Whether this node should be be visible to the UI
            if (node.isHidden()) {
                JsonArray update = new JsonArray();
                update.add("$hidden");
                update.add(true);
                updates.add(update);
            }

            // Children
            Map<String, Node> children = node.getChildren();
            if (children != null) {
                for (Node child : children.values()) {
                    Object update = getChildUpdate(child, false);
                    updates.add(update);
                }
            }
        }
        out.put("updates", updates);

        manager.addPathSub(path, this);
        return out;
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

    /**
     * @return Children updates. The key is the updated node, the bool is
     * {@code true} if the node was removed, otherwise false.
     */
    public Map<Node, Boolean> getUpdates() {
        return updates;
    }

    public void metaUpdate(String name, Value value) {
        JsonArray updates = new JsonArray();
        if (value != null) {
            JsonArray update = new JsonArray();
            update.add(name);

            update.add(value);
            update.add(value.getTimeStamp());

            updates.add(update);
        } else {
            JsonObject obj = new JsonObject();
            obj.put("name", name);
            obj.put("change", "remove");
            updates.add(obj);
        }

        JsonObject resp = new JsonObject();
        resp.put("rid", getRid());
        resp.put("stream", StreamState.OPEN.getJsonName());
        resp.put("updates", updates);
        link.getWriter().writeResponse(resp);
    }

    public void multiChildrenUpdate(List<Node> children) {
        JsonArray updates = new JsonArray();

        for (Node child : children) {
            updates.add(getChildUpdate(child, false));
        }

        JsonObject resp = new JsonObject();
        resp.put("rid", getRid());
        resp.put("stream", StreamState.OPEN.getJsonName());
        resp.put("updates", updates);
        link.getWriter().writeResponse(resp);
    }

    @Override
    public void populate(JsonObject in) {
        JsonArray updates = in.get("updates");
        if (updates != null) {
            for (Object obj : updates) {
                if (obj instanceof JsonObject) {
                    update((JsonObject) obj);
                } else {
                    update((JsonArray) obj);
                }
            }
        }
    }

    /**
     * @param prefix Prefix to use (whether its an attribute or config)
     * @param out    Updates array
     * @param values Values to iterate and add to the updates array
     */
    private void add(String prefix, JsonArray out, Map<String, Value> values) {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, Value> entry : values.entrySet()) {
            JsonArray update = new JsonArray();
            update.add(prefix + entry.getKey());
            update.add(entry.getValue());
            out.add(update);
        }
    }

    private Object getChildUpdate(Node child, boolean removed) {
        if (removed) {
            JsonObject obj = new JsonObject();
            obj.put("name", child.getName());
            obj.put("change", "remove");
            return obj;
        }

        JsonArray update = new JsonArray();
        update.add(child.getName());

        JsonObject childData = new JsonObject();
        {
            String profile = child.getProfile();
            if (profile == null) {
                String err = "Profile not set on node: "
                        + child.getPath();
                throw new RuntimeException(err);
            }
            childData.put("$is", profile);

            String displayName = child.getDisplayName();
            if (displayName != null) {
                childData.put("$name", displayName);
            }

            Action action = child.getAction();
            if (action != null) {
                String perm = action.getPermission().getJsonName();
                childData.put("$invokable", perm);

                String jsonName = action.getResultType().getJsonName();
                childData.put("$result", jsonName);
            }

            Set<String> interfaces = child.getInterfaces();
            if (interfaces != null) {
                String _interface = StringUtils.join(interfaces, "|");
                childData.put("$interface", _interface);
            }

            ValueType type = child.getValueType();
            if (type != null) {
                childData.put("$type", type.toJsonString());
            }

            Boolean hasChildren = child.getHasChildren();
            if (hasChildren != null) {
                childData.put("$hasChildren", hasChildren);
            }

            if (child.isHidden()) {
                childData.put("$hidden", true);
            }
        }
        update.add(childData);
        return update;
    }

    private static Action getOrCreateAction(Node node, Permission perm) {
        Action action = node.getAction();
        if (action != null) {
            return action;
        }

        action = getRawAction(perm);
        node.setAction(action);
        return action;
    }

    private static Action getRawAction(Permission perm) {
        return new Action(perm, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                throw new UnsupportedOperationException();
            }
        });
    }

    private static void iterateActionMetaData(Action act,
                                              JsonArray array,
                                              boolean isCol) {
        for (Object anArray : array) {
            JsonObject data = (JsonObject) anArray;
            String name = data.get("name");
            String type = data.get("type");
            ValueType valType = ValueType.toValueType(type);
            Parameter param = new Parameter(name, valType);

            String editor = data.get("editor");

            if (editor != null) {
                JsonObject editorMeta = data.get("editorMeta");
                param.setEditorType(EditorType.make(editor, editorMeta));
            }

            Object def = data.get("default");
            if (def != null) {
                param.setDefaultValue(ValueUtils.toValue(def));
            }

            String description = data.get("description");
            String placeholder = data.get("placeholder");

            if (description != null) {
                param.setDescription(description);
            }

            if (placeholder != null) {
                param.setPlaceHolder(placeholder);
            }

            if (isCol) {
                act.addResult(param);
            } else {
                act.addParameter(param);
            }
        }
    }

    private void update(JsonArray in) {
        String name = in.get(0);
        Object v = in.get(1);

        if (name.startsWith("$$")) {
            name = name.substring(2);
            if ("password".equals(name)) {
                if (v != null && v instanceof String) {
                    node.setPassword(((String) v).toCharArray());
                } else {
                    node.setPassword(null);
                }
            } else {
                if (v != null) {
                    node.setRoConfig(name, ValueUtils.toValue(v));
                } else {
                    node.removeRoConfig(name);
                }
            }
        } else if (name.startsWith("$")) {
            name = name.substring(1);
            if ("is".equals(name)) {
                if (v == null) {
                    v = "node"; // $is should always have a value. A null value is a bug in the responder that sent it.
                }
                node.reset();
                node.setProfile((String) v);
            } else if ("interface".equals(name)) {
                node.setInterfaces((String) v);
            } else if ("invokable".equals(name)) {
                Permission perm = Permission.toEnum((String) v);
                Action act = getOrCreateAction(node, perm);
                act.setPermission(perm);
            } else if ("params".equals(name)) {
                if (v instanceof JsonArray) {
                    JsonArray array = (JsonArray) v;
                    Action act = getOrCreateAction(node, Permission.NONE);
                    iterateActionMetaData(act, array, false);
                }
            } else if ("columns".equals(name)) {
                if (v instanceof JsonArray) {
                    JsonArray array = (JsonArray) v;
                    Action act = getOrCreateAction(node, Permission.NONE);
                    iterateActionMetaData(act, array, true);
                }
            } else if ("result".equals(name)) {
                String string = (String) v;
                Action act = getOrCreateAction(node, Permission.NONE);
                act.setResultType(ResultType.toEnum(string));
            } else if ("writable".equals(name)) {
                String string = (String) v;
                node.setWritable(Writable.toEnum(string));
            } else if ("type".equals(name)) {
                ValueType type = ValueType.toValueType((String) v);
                if (!type.equals(node.getValueType())) {
                    node.setValueType(type);
                }
            } else if ("name".equals(name)) {
                node.setDisplayName((String) v);
            } else if ("hidden".equals(name)) {
                node.setHidden((Boolean) v);
            } else {
                node.setConfig(name, ValueUtils.toValue(v));
            }
        } else if (name.startsWith("@")) {
            name = name.substring(1);
            node.setAttribute(name, ValueUtils.toValue(v));
        } else {
            // Child node
            JsonObject childData = (JsonObject) v;
            Node child = node.getChild(name, false);

            String change = childData.get("change");
            if (change != null && "remove".equals(change)) {
                if (child != null) {
                    Node n = node.removeChild(child.getName());
                    updates.put(n, true);
                }
                return;
            }

            String is = childData.get("$is");
            if (child == null) {
                NodeBuilder builder = node.createChild(name, is);
                child = builder.build();
            }

            String _interface = childData.get("$interface");
            if (_interface != null) {
                child.setInterfaces(_interface);
            }

            String displayName = childData.get("$name");
            if (displayName != null) {
                child.setDisplayName(displayName);
            }

            String type = childData.get("$type");
            if (type != null) {
                ValueType t = ValueType.toValueType(type);
                child.setValueType(t);
            }

            String invokable = childData.get("$invokable");
            if (invokable != null) {
                Permission perm = Permission.toEnum(invokable);
                getOrCreateAction(child, perm);
            }

            Boolean hidden = childData.get("$hidden");
            if (hidden != null) {
                child.setHidden(hidden);
            }

            JsonObject linkData = childData.get("$linkData");
            if (linkData != null) {
                Value val = new Value(linkData);
                child.setConfig("linkData", val);
            }

            String result = childData.get("$result");
            if (result != null) {
                Action action = getOrCreateAction(child, Permission.NONE);
                action.setResultType(ResultType.toEnum(result));
            }

            Object params = childData.get("$params");
            if (params instanceof JsonArray) {
                Action action = getOrCreateAction(child, Permission.NONE);
                iterateActionMetaData(action, (JsonArray) params, false);
            }

            Object columns = childData.get("$columns");
            if (columns instanceof JsonArray) {
                Action action = getOrCreateAction(child, Permission.NONE);
                iterateActionMetaData(action, (JsonArray) columns, true);
            }

            updates.put(child, false);
        }
    }

    private void update(JsonObject in) {
        String name = in.get("name");
        String change = in.get("change");
        if (change != null) {
            if ("remove".equals(change)) {
                if (name.startsWith("$$")) {
                    node.removeRoConfig(name.substring(2));
                } else if (name.startsWith("$")) {
                    node.removeConfig(name.substring(1));
                } else if (name.startsWith("@")) {
                    node.removeAttribute(name.substring(1));
                } else {
                    Node n = node.removeChild(name);
                    if (n != null) {
                        updates.put(n, true);
                    }
                }
            }
        } else {
            throw new RuntimeException("Unhandled update: " + in);
        }
    }
}
