package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.Permission;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ListResponse implements Response {

    private final int rid;
    private final Node node;

    public ListResponse(int rid, Node node) {
        if (rid <= 0)
            throw new IllegalArgumentException("rid <= 0");
        else if (node == null)
            throw new NullPointerException("node");
        this.rid = rid;
        this.node = node;
    }

    @Override
    public int getRid() {
        return rid;
    }

    @Override
    public void populate(JsonArray in) {
        for (Object obj : in) {
            update((JsonArray) obj);
        }
    }

    private void update(JsonArray in) {
        String name = in.get(0);
        Object v = in.get(1);

        char start = name.charAt(0);
        if (start == '$') {
            name = name.substring(1);
            node.setConfig(name, ValueUtils.toValue(v));
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

            if (child == null)
                child = node.createChild(name);

            String is = childData.getString("$is");
            if (is != null) {
                child.setConfig("is", new Value(is));
            }

            String displayName = childData.getString("$name");
            if (displayName != null) {
                child.setDisplayName(displayName);
            }

            String type = childData.getString("$type");
            if (type != null) {
                child.setConfig("type", new Value(type));
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
}
