package org.dsa.iot.dslink.responses;

import lombok.Getter;
import lombok.val;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requests.ListRequest;
import org.dsa.iot.dslink.util.Permission;
import org.dsa.iot.dslink.util.ValueUtils;
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
            Object v = nodeData.get(1);

            Value value = null;
            if (!(v instanceof JsonObject)) {
                value = ValueUtils.toValue(v);
            }
            char start = name.charAt(0);
            if (start == '$') {
                name = name.substring(1);
                node.setConfiguration(name, value);
            } else if (start == '@') {
                name = name.substring(1);
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
                    continue;
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
                    child.setInvokable(Permission.toEnum(invokable));
                }

                String interfaces = childData.getString("$interface");
                child.clearInterfaces();
                if (interfaces != null) {
                    String[] split = interfaces.split("\\|");
                    for (String i : split) {
                        child.addInterface(i);
                    }
                }
            }
        }
    }
}
