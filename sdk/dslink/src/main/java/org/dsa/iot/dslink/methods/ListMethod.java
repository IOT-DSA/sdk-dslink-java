package org.dsa.iot.dslink.methods;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.dslink.Responder;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.StreamState;
import org.dsa.iot.dslink.util.ValueUtils;
import org.dsa.iot.dslink.util.Client;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class ListMethod extends Method {

    @NonNull private final Responder responder;
    @NonNull private final Client client;
    @NonNull private final Node parent;
    private final int rid;

    @Override
    public JsonArray invoke(JsonObject request) {
        setState(StreamState.OPEN);
        JsonArray resp = getResponse();
        parent.subscribeToChildren(client, responder, rid);
        return resp;
    }

    private JsonArray getResponse() {
        JsonArray array = new JsonArray();
        writeParentData(array, "$", parent.getConfigurations());
        writeParentData(array, "@", parent.getAttributes());

        Map<String, Node> children = parent.getChildren();
        if (children != null) {
            for (Node node : children.values()) {
                array.addArray(getChildUpdate(node, false));
            }
        }
        return array;
    }

    private void writeParentData(JsonArray out, String prefix,
                                    Map<String, Value> data) {
        if (data != null) {
            for (Map.Entry<String, Value> entry : data.entrySet()) {
                JsonArray valArray = new JsonArray();
                valArray.addString(prefix + entry.getKey());

                Value value = entry.getValue();
                ValueUtils.toJson(valArray, value);
                out.addElement(valArray);
            }
        }
    }

    public static JsonArray getChildUpdate(Node node, boolean removed) {
        JsonArray array = new JsonArray();
        array.addString(node.getName());

        {
            JsonObject obj = new JsonObject();
            obj.putString("$is", node.getConfiguration("is").getString());

            String name = node.getDisplayName();
            if (name != null) {
                obj.putString("$name", name);
            }

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
}
