package org.dsa.iot.dslink.node.value;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * Utilities for manipulating values.
 *
 * @author Samuel Grenier
 */
public class ValueUtils {

    private static final String ERROR_MSG = "Unhandled value type: ";

    /**
     * Converts an {@link Object} to a {@link Value}.
     *
     * @param object Object to convert.
     * @return Converted object instance.
     */
    @SuppressWarnings("unchecked")
    public static Value toValue(Object object) {
        if (object == null) {
            return null;
        }
        Value val;
        if (object instanceof Number) {
            val = new Value((Number) object);
        } else if (object instanceof Boolean) {
            val = new Value(((Boolean) object));
        } else if (object instanceof String) {
            val = new Value((String) object);
        } else if (object instanceof JsonObject) {
            val = new Value((JsonObject) object);
        } else if (object instanceof Map) {
            val = new Value(new JsonObject((Map) object));
        } else if (object instanceof JsonArray) {
            val = new Value((JsonArray) object);
        } else if (object instanceof List) {
            val = new Value(new JsonArray((List) object));
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
        switch (value.getType().toJsonString()) {
            case ValueType.JSON_BOOL:
                array.addBoolean(value.getBool());
                break;
            case ValueType.JSON_NUMBER:
                array.addNumber(value.getNumber());
                break;
            case ValueType.JSON_STRING:
                array.addString(value.getString());
                break;
            case ValueType.JSON_MAP:
                array.addObject(value.getMap());
                break;
            case ValueType.JSON_ARRAY:
                array.addArray(value.getArray());
                break;
            case ValueType.JSON_ENUM:
                array.addString(value.toString());
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
        switch (value.getType().toJsonString()) {
            case ValueType.JSON_BOOL:
                object.putBoolean(name, value.getBool());
                break;
            case ValueType.JSON_NUMBER:
                object.putNumber(name, value.getNumber());
                break;
            case ValueType.JSON_STRING:
                object.putString(name, value.getString());
                break;
            case ValueType.JSON_MAP:
                JsonObject map = value.getMap();
                object.putObject(name, map);
                break;
            case ValueType.JSON_ARRAY:
                JsonArray arr = value.getArray();
                object.putArray(name, arr);
                break;
            case ValueType.JSON_ENUM:
                object.putString(name, value.toString());
                break;
            default:
                throw new RuntimeException(ERROR_MSG + value.getType());
        }
    }

    /**
     * Tests the type of the {@code value} to see whether its type is valid
     * or not.
     *
     * @param name Name of the parameter, used when throwing
     *             {@link RuntimeException}.
     * @param type The {@link ValueType} that {@code value} must be.
     * @param value The {@link Value} to test.
     */
    public static void checkType(String name, ValueType type, Value value) {
        ValueType vt = value.getType();
        if (!type.compare(ValueType.DYNAMIC)
                && (type.compare(ValueType.ENUM)
                && !vt.compare(ValueType.STRING))) {
            if (!type.compare(vt)) {
                String t = value.getType().toJsonString();
                String msg = "Parameter " + name + " has a bad type of " + t;
                msg += " expected " + type.getRawName();
                throw new RuntimeException(msg);
            }
        }
    }
}
