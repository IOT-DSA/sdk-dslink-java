package org.dsa.iot.dslink.node;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class Subscription {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Getter @NonNull private final Client client;
    
    public void update(List<Node> nodes) {
        if (nodes == null)
            return;
        List<JsonElement> list = new ArrayList<>(nodes.size());
        for (Node node : nodes) {
            list.add(update(node, false));
        }
        send(list.toArray(new JsonElement[list.size()]));
    }

    public void update(Node node) {
        update(node, true);
    }

    private synchronized JsonElement update(Node node, boolean send) {
        if (node == null)
            return null;
        JsonArray array = new JsonArray();
        array.addString(node.getPath());

        Value value = node.getValue();
        if (value != null) {
            ValueUtils.toJson(array, value);
        } else {
            array.add(null);
        }

        array.addString(FORMAT.format(new Date()));
        if (send) {
            send(array);
        }
        return array;
    }

    private void send(JsonElement... updateArray) {
        JsonObject obj = new JsonObject();
        obj.putNumber("rid", 0);

        JsonArray updates = new JsonArray();
        for (JsonElement update : updateArray) {
            updates.addElement(update);
        }

        obj.putArray("updates", updates);

        JsonArray array = new JsonArray();
        array.add(obj);

        JsonObject response = new JsonObject();
        response.putArray("responses", array);

        client.write(response);
        System.out.println(client.getDsId() + " => " + response.encode());
    }
    
}
