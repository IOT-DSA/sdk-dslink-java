package org.dsa.iot.dslink.methods;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.dslink.connection.Connector;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.RequestTracker;
import org.dsa.iot.dslink.node.value.Value;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

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
    private final RequestTracker tracker;

    private final int rid;

    private Handler<NodeManager.NodeBooleanTuple> handler;

    @Override
    public JsonObject invoke(JsonObject request) {
        handler = parent.addChildrenHandler(new Handler<NodeManager.NodeBooleanTuple>() {
            @Override
            public void handle(NodeManager.NodeBooleanTuple event) {
                nodeUpdate(event.getNode(), event.isBool());
            }
        });
        setState(StreamState.OPEN);
        return getResponse().asObject();
    }

    public void nodeUpdate(Node node, boolean removed) {
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
            for (Map.Entry<String, Node> entry : children.entrySet()) {
                JsonArray arr = new JsonArray();
                arr.addString(entry.getKey());

                Node node = entry.getValue();
                arr.addElement(getChild(node, false));

                array.addElement(arr);
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

            Value interfaces = node.getConfiguration("interface");
            if (interfaces != null) {
                obj.putString("$interface", interfaces.getString());
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
                switch (value.getType()) {
                    case STRING:
                        valArray.addString(value.getString());
                        break;
                    case NUMBER:
                        valArray.addNumber(value.getInteger());
                        break;
                    case BOOL:
                        valArray.addBoolean(value.getBool());
                        break;
                    default:
                        throw new RuntimeException("Unhandled value type");
                }
                out.addElement(valArray);
            }
        }
    }
}
