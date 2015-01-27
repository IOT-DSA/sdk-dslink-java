package org.dsa.iot.dslink.requests;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class InvokeRequest extends Request {

    @NonNull
    private final String path;

    @NonNull
    private final JsonObject params;

    @Override
    public String getName() {
        return "invoke";
    }

    @Override
    public void addJsonValues(JsonObject obj) {
        obj.putString("path", path);
        obj.putObject("params", params);
    }
}
