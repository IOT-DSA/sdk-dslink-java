package org.dsa.iot.dslink.requester.requests;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class SetRequest extends Request {

    @NonNull
    private final String path;

    @NonNull
    private final Value value;

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public void addJsonValues(JsonObject obj) {
        obj.putString("path", path);
        ValueUtils.toJson(obj, "value", value);
    }
}
