package org.dsa.iot.dslink.responses;

import lombok.Getter;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requests.ListRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
public class ListResponse extends Response<ListRequest> {

    private final NodeManager manager;

    private String path;

    public ListResponse(ListRequest request, NodeManager manager) {
        super(request);
        this.manager = manager;
    }

    @Override
    public void populate(JsonArray o) {
        path = getRequest().getPath();
        Node node = manager.getNode(path, true).getNode();
        for (Object obj : o) {
            JsonArray nodeData = (JsonArray) obj;
            String name = nodeData.get(0);
            Object val = nodeData.get(1);

            Value value = null;
            if (val instanceof Number) {
                value = new Value(((Number) val).intValue());
            } else if (val instanceof Boolean) {
                value = new Value(((Boolean) val));
            } else if (val instanceof String) {
                value = new Value((String) val);
            } else if (!(val instanceof JsonObject)) {
                throw new RuntimeException("Unhandled type");
            }

            char start = name.charAt(0);
            name = name.substring(1);
            if (start == '$') {
                node.setConfiguration(name, value);
            } else if (start == '@') {
                node.setAttribute(name, value);
            } else {
                // Child node
                @SuppressWarnings("ConstantConditions")
                JsonObject childData = (JsonObject) val;
                Node child = node.getChild(name);

                String change = childData.getString("change");
                if (change != null && "remove".equals(change)) {
                    if (child != null) {
                        node.removeChild(child);
                    }
                    continue;
                }

                if (child == null)
                    child = node.createChild(name);

                String is = childData.getString("$is");
                if (is != null) {
                    child.setConfiguration("is", new Value(is));
                }

                child.setDisplayName(childData.getString("$name"));

                Boolean invokable = childData.getBoolean("$invokable");
                if (invokable != null) {
                    child.setInvokable(invokable);
                }

                // TODO interfaces
            }
        }
    }
}
