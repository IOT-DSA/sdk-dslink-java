package org.dsa.iot.responder.methods;

import org.dsa.iot.responder.node.Node;
import org.dsa.iot.responder.node.exceptions.NoSuchPathException;
import org.dsa.iot.responder.node.value.Value;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class ListMethod extends Method {

    private final Node parent;

    /**
     * @param parent The node to be listed
     */
    public ListMethod(Node parent) {
        if (parent == null) {
            throw new NoSuchPathException();
        }
        this.parent = parent;
    }

    @Override
    public JsonObject invoke() {
        JsonArray array = new JsonArray();
        writeParentData(array, parent.getConfigurations());
        writeParentData(array, parent.getAttributes());

        Map<String, Node> children = parent.getChildren();
        if (children != null) {
            for (Map.Entry<String, Node> entry : children.entrySet()) {
                JsonArray arr = new JsonArray();
                arr.addString(entry.getKey());

                Node node = entry.getValue();

                JsonObject obj = new JsonObject();
                {
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
                }
                arr.addElement(obj);

                array.addElement(arr);
            }
        }

        return array.asObject();
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
                        valArray.addBoolean(value.getBoolean());
                        break;
                    default:
                        throw new RuntimeException("Unhandled value type");
                }
                out.addElement(valArray);
            }
        }
    }
}
