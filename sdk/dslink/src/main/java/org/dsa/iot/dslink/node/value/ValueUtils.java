package org.dsa.iot.dslink.node.value;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Utilities for manipulating values.
 *
 * @author Samuel Grenier
 */
public class ValueUtils {

    private static final String ERROR_MSG = "Unhandled value type: ";

    /**
     * Converts a string type to a value type with a null underlying
     * value.
     *
     * @param type Type to set on the value
     * @return Type to value
     */
    public static Value fromType(String type) {
        if (type == null) {
            throw new NullPointerException("type");
        }

        ValueType switchType = ValueType.toEnum(type);
        switch (switchType) {
            case BOOL:
                return new Value((Boolean) null);
            case NUMBER:
                return new Value((Number) null);
            case STRING:
                return new Value((String) null);
            case MAP:
                return new Value((JsonObject) null);
            case ARRAY:
                return new Value((JsonArray) null);
            default:
                throw new RuntimeException(ERROR_MSG + switchType);
        }
    }

    /**
     * @param object Object to convert
     * @return Converted object instance
     */
    public static Value toValue(Object object) {
        if (object == null)
            throw new NullPointerException("object");
        Value val;
        if (object instanceof Number) {
            val = new Value((Number) object);
        } else if (object instanceof Boolean) {
            val = new Value(((Boolean) object));
        } else if (object instanceof String) {
            val = new Value((String) object);
        } else if (object instanceof JsonObject) {
            val = new Value((JsonObject) object);
        } else if (object instanceof JsonArray) {
            val = new Value((JsonArray) object);
        } else {
            throw new RuntimeException(ERROR_MSG + object.getClass().getName());
        }
        return val;
    }

    /**
     * @param array JSON array to modify.
     * @param value Value to inject into the JSON array.
     */
    public static void toJson(JsonArray array, Value value) {
        if (array == null)
            throw new NullPointerException("array");
        else if (value == null)
            throw new NullPointerException("value");
        switch (value.getType()) {
            case BOOL:
                array.addBoolean(value.getBool());
                break;
            case NUMBER:
                array.addNumber(value.getNumber());
                break;
            case STRING:
                array.addString(value.getString());
                break;
            case MAP:
                array.addObject(value.getMap());
                break;
            case ARRAY:
                array.addArray(value.getArray());
                break;
            default:
                throw new RuntimeException(ERROR_MSG + value.getType());
        }
    }

    /**
     * Inserts the name and value pair into the object after converting
     * the value to be json compatible.
     *
     * @param object JSON object to modify
     * @param name   Name to put into the object
     * @param value  Value to put into the object
     */
    public static void toJson(JsonObject object, String name, Value value) {
        if (object == null)
            throw new NullPointerException("object");
        else if (name == null)
            throw new NullPointerException("name");
        else if (value == null)
            throw new NullPointerException("value");
        switch (value.getType()) {
            case BOOL:
                object.putBoolean(name, value.getBool());
                break;
            case NUMBER:
                object.putNumber(name, value.getNumber());
                break;
            case STRING:
                object.putString(name, value.getString());
                break;
            case MAP:
                JsonObject map = value.getMap();
                object.putObject(name, map);
                break;
            case ARRAY:
                JsonArray arr = value.getArray();
                object.putArray(name, arr);
                break;
            default:
                throw new RuntimeException(ERROR_MSG + value.getType());
        }
    }
}
