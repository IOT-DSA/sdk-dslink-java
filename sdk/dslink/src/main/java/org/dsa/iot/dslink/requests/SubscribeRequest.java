package org.dsa.iot.dslink.requests;

import lombok.NonNull;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class SubscribeRequest extends Request {

    private final String[] paths;

    public SubscribeRequest(@NonNull String path) {
        this(new String[] { path });
    }

    public SubscribeRequest(@NonNull String[] paths) {
        this.paths = paths;
    }

    @Override
    public String getName() {
        return "subscribe";
    }

    @Override
    public void addJsonValues(JsonObject obj) {
        JsonArray array = new JsonArray();
        for (String path : paths) {
            array.addString(path);
        }
        obj.putArray("paths", array);
    }
}
