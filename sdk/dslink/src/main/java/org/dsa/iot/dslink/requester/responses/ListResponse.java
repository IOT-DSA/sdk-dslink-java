package org.dsa.iot.dslink.requester.responses;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.requester.requests.ListRequest;
import org.dsa.iot.dslink.responder.action.Action;
import org.dsa.iot.dslink.util.Permission;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
public class ListResponse extends Response<ListRequest> {

    private final NodeManager manager;

    @Setter(AccessLevel.PROTECTED)
    private String path;

    public ListResponse(ListRequest request, NodeManager manager) {
        super(request);
        this.manager = manager;
    }

    @Override
    public void populate(JsonArray array) {
        path = getRequest().getPath();
        Node node = manager.getNode(path, true).getKey();
        iterate(node, array);
    }

    protected void iterate(Node node, JsonArray array) {
        for (Object obj : array) {
            JsonArray nodeData = (JsonArray) obj;
            update(node, nodeData);
        }
    }

    protected void update(Node node, JsonArray nodeData) {
        String name = nodeData.get(0);
        Object v = nodeData.get(1);

        Value value;
        char start = name.charAt(0);
        if (start == '$') {
            name = name.substring(1);
            value = ValueUtils.toValue(v);
            node.setConfiguration(name, value);
        } else if (start == '@') {
            name = name.substring(1);
            value = ValueUtils.toValue(v);
            node.setAttribute(name, value);
        } else {
            // Child node
            @SuppressWarnings("ConstantConditions")
            val childData = (JsonObject) v;
            Node child = node.getChild(name);

            val change = childData.getString("change");
            if (change != null && "remove".equals(change)) {
                if (child != null) {
                    node.removeChild(child);
                }
                return;
            }

            if (child == null)
                child = node.createChild(name);

            String is = childData.getString("$is");
            if (is != null) {
                child.setConfiguration("is", new Value(is));
            }

            child.setDisplayName(childData.getString("$name"));

            val invokable = childData.getString("$invokable");
            if (invokable != null) {
                val perm = Permission.toEnum(invokable);
                child.setAction(new Action(perm, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                        throw new UnsupportedOperationException();
                    }
                }));
            }

            String interfaces = childData.getString("$interface");
            child.clearInterfaces();
            if (interfaces != null) {
                String[] split = interfaces.split("\\|");
                for (String i : split) {
                    child.addInterface(i);
                }
            }

            String type = childData.getString("$type");
            if (type != null) {
                child.setConfiguration("type", new Value(type));
            }
        }
    }
}
