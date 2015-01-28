package org.dsa.iot.dslink.methods;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.dslink.connection.Connector;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.ResponseTracker;
import org.dsa.iot.dslink.util.StreamState;
import org.dsa.iot.dslink.util.ValueUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

import static org.dsa.iot.dslink.node.NodeManager.NodeBooleanTuple;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class ListMethod extends Method {

    @NonNull
    private final Connector connector;

    @NonNull
    private final Node parent;

    @NonNull
    private final ResponseTracker tracker;

    private final int rid;

    private Handler<NodeManager.NodeBooleanTuple> handler;

    @Override
    public JsonArray invoke(JsonObject request) {
        handler = parent.addChildrenHandler(new Handler<NodeBooleanTuple>() {
            @Override
            public void handle(NodeManager.NodeBooleanTuple event) {
                nodeUpdate(event.getNode(), event.isBool());
            }
        });
        setState(StreamState.OPEN);
        return getResponse();
    }

    private void nodeUpdate(Node node, boolean removed) {
        if (tracker.isTracking(rid)) {
            JsonObject response = new JsonObject();
            response.putNumber("rid", rid);
            response.putString("stream", getState().jsonName);

            JsonArray updates = new JsonArray();
            updates.addElement(getChild(node, removed));
            response.putArray("update", updates);

            connector.write(response);
        } else {
            parent.removeChildrenHandler(handler);
        }
    }

    private JsonArray getResponse() {
        JsonArray array = new JsonArray();
        writeParentData(array, parent.getConfigurations());
        writeParentData(array, parent.getAttributes());

        Map<String, Node> children = parent.getChildren();
        if (children != null) {
            for (Node node : children.values()) {
                array.addArray(getChild(node, false));
            }
        }
        return array;
    }

    private JsonArray getChild(Node node, boolean removed) {
        JsonArray array = new JsonArray();
        array.addString(node.getName());

        {
            JsonObject obj = new JsonObject();
            obj.putString("$is", node.getConfiguration("is").getString());

            String name = node.getDisplayName();
            if (name != null) {
                obj.putString("$name", name);
            }

            obj.putBoolean("$invokable", node.isInvokable());

            List<String> interfaces = node.getInterfaces();
            StringBuilder builder = new StringBuilder();
            if (interfaces != null) {
                for (String i : interfaces) {
                    builder.append(i);
                    builder.append("|");
                }
                String built = builder.substring(0, builder.length() - 1);
                obj.putString("$interface", built);
            }

            if (removed) {
                obj.putString("$change", "remove");
            }

            array.addObject(obj);
        }
        return array;
    }

    private void writeParentData(JsonArray out, Map<String, Value> data) {
        if (data != null) {
            for (Map.Entry<String, Value> entry : data.entrySet()) {
                JsonArray valArray = new JsonArray();
                valArray.addString(entry.getKey());

                Value value = entry.getValue();
                ValueUtils.toJson(valArray, value);
                out.addElement(valArray);
            }
        }
    }
}
