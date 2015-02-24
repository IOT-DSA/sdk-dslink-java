package org.dsa.iot.dslink.node.value;

import lombok.NonNull;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ValueUtils {

    private static final String ERROR_MSG = "Unhandled value type: ";

    public static Value toValue(@NonNull Object o) {
        Value val;
        if (o instanceof Number) {
            val = new Value(((Number) o).intValue());
        } else if (o instanceof Boolean) {
            val = new Value(((Boolean) o));
        } else if (o instanceof String) {
            val = new Value((String) o);
        } else {
            throw new RuntimeException(ERROR_MSG + o.getClass().getName());
        }
        return val;
    }

    public static void toJson(@NonNull JsonArray array,
                              @NonNull Value value) {
        switch (value.getType()) {
            case BOOL:
                array.addBoolean(value.getBool());
                break;
            case NUMBER:
                array.addNumber(value.getInteger());
                break;
            case STRING:
                array.addString(value.getString());
                break;
            default:
                throw new RuntimeException(ERROR_MSG + value.getType());
        }
    }

    public static void toJson(@NonNull JsonObject object,
                              @NonNull String name,
                              @NonNull Value value) {
        switch (value.getType()) {
            case BOOL:
                object.putBoolean(name, value.getBool());
                break;
            case NUMBER:
                object.putNumber(name, value.getInteger());
                break;
            case STRING:
                object.putString(name, value.getString());
                break;
            default:
                throw new RuntimeException(ERROR_MSG + value.getType());
        }
    }
}
