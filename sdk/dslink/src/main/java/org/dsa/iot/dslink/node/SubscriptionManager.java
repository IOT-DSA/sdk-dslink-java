package org.dsa.iot.dslink.node;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class SubscriptionManager {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @NonNull
    private final ClientConnector connector;

    //private final List<JsonObject> updates = new ArrayList<>();

    public synchronized void update(Node node) {
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
        send(array.asObject());
    }

    private void send(JsonObject update) {
        JsonObject obj = new JsonObject();
        obj.putNumber("rid", 0);

        JsonArray updates = new JsonArray();
        /*
        for (JsonObject update : this.updates) {
            updates.addElement(update);
        }
        this.updates.clear();
        */
        updates.addObject(update);

        obj.putArray("updates", updates);

        connector.write(obj);
    }
}
