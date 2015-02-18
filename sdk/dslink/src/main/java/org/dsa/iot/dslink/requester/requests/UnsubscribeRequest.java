package org.dsa.iot.dslink.requester.requests;

import lombok.NonNull;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class UnsubscribeRequest extends Request {

    private final String[] paths;

    public UnsubscribeRequest(@NonNull String path) {
        this(new String[] { path });
    }

    public UnsubscribeRequest(@NonNull String[] paths) {
        this.paths = paths.clone();
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
