package org.dsa.iot.dslink.node;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.ClientConnector;
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
@AllArgsConstructor
public class SubscriptionManager {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @NonNull
    private final ClientConnector connector;

    //private final List<JsonObject> updates = new ArrayList<>();

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

        //updates.add(array.asObject());
        if (send) {
            send(array);
        }
        return array;
    }

    private void send(JsonElement... updateArray) {
        JsonObject obj = new JsonObject();
        obj.putNumber("rid", 0);

        JsonArray updates = new JsonArray();
        /*
        for (JsonObject update : this.updates) {
            updates.addElement(update);
        }
        this.updates.clear();
        */
        for (JsonElement update : updateArray) {
            updates.addElement(update);
        }

        obj.putArray("updates", updates);

        connector.write(obj);
    }
}
