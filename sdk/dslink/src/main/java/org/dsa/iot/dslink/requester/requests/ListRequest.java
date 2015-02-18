package org.dsa.iot.dslink.requester.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class ListRequest extends Request {

    @Getter
    @NonNull
    private final String path;

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public void addJsonValues(JsonObject obj) {
        obj.putString("path", path);
    }
}
