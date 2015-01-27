package org.dsa.iot.dslink.requests;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.dslink.node.value.Value;
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
        switch (value.getType()) {
            case BOOL:
                obj.putBoolean("value", value.getBool());
                break;
            case NUMBER:
                obj.putNumber("value", value.getInteger());
                break;
            case STRING:
                obj.putString("value", value.getString());
                break;
            default:
                throw new RuntimeException("Unhandled type: " + value.getType());
        }
    }
}
