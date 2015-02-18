package org.dsa.iot.dslink.requester.requests;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class RemoveRequest extends Request {

    @NonNull
    private final String path;

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public void addJsonValues(JsonObject obj) {
        obj.putString("path", path);
    }
}
