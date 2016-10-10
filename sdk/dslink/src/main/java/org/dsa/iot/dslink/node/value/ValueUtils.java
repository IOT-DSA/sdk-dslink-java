package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

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
     * Creates a new copy of the the argument and leaves it mutable.
     */
    public static Value mutableCopy(Value arg) {
        ValueType type = arg.getType();
        if (ValueType.NUMBER.compare(type)) {
            return new Value(arg.getNumber(), arg.getTimeStamp());
        } else if (ValueType.BOOL.compare(type)) {
            return new Value(arg.getBool(), arg.getTimeStamp());
        } else if (ValueType.STRING.compare(type)) {
            return new Value(arg.getString(), arg.getTimeStamp());
        } else if (ValueType.MAP.compare(type)) {
            return new Value(arg.getMap(), arg.getTimeStamp());
        } else if (ValueType.ARRAY.compare(type)) {
            return new Value(arg.getArray(), arg.getTimeStamp());
        } else if (ValueType.BINARY.compare(type)) {
            return new Value(arg.getBinary(), arg.getTimeStamp());
        }
        return new Value((String) null);
    }

    /**
     * Creates an empty value with the designated type.
     *
     * @param type Type of the value.
     * @param time Time of the value.
     * @return An empty value with the designated time.
     */
    public static Value toEmptyValue(ValueType type, String time) {
        if (type.compare(ValueType.NUMBER)) {
            return new Value((Number) null, time);
        } else if (type.compare(ValueType.BOOL)) {
            return new Value((Boolean) null, time);
        } else if (type.compare(ValueType.STRING)) {
            return new Value((String) null, time);
        } else if (type.compare(ValueType.MAP)) {
            return new Value((JsonObject) null, time);
        } else if (type.compare(ValueType.ARRAY)) {
            return new Value((JsonArray) null, time);
        } else if (type.compare(ValueType.BINARY)) {
            return new Value((byte[]) null, time);
        }
        return new Value((String) null);
    }

    /**
     * Converts an {@link Object} to a {@link Value}. An initial timestamp
     * will not be set.
     *
     * @param object Object to convert.
     * @return Converted object instance.
     */
    public static Value toValue(Object object) {
        return toValue(object, null);
    }

    /**
     * Converts an {@link Object} to a {@link Value}.
     *
     * @param object Object to convert.
     * @param time Initial time of the value.
     * @return Converted object instance.
     */
    @SuppressWarnings("unchecked")
    public static Value toValue(Object object, String time) {
        if (object == null) {
            return null;
        }
        Value val;
        if (object instanceof Number) {
            val = new Value((Number) object, time);
        } else if (object instanceof Boolean) {
            val = new Value(((Boolean) object), time);
        } else if (object instanceof String) {
            val = new Value((String) object, time);
        } else if (object instanceof JsonObject) {
            val = new Value((JsonObject) object, time);
        } else if (object instanceof Map) {
            val = new Value(new JsonObject((Map) object), time);
        } else if (object instanceof JsonArray) {
            val = new Value((JsonArray) object, time);
        } else if (object instanceof List) {
            val = new Value(new JsonArray((List) object), time);
        } else if (object instanceof byte[]) {
            val = new Value((byte[]) object, time);
        } else {
            throw new RuntimeException(ERROR_MSG + object.getClass().getName());
        }
        return val;
    }

    /**
     * @param value Value to convert.
     * @return Converted {@link Value} to its raw {@link Object} form.
     */
    public static Object toObject(Value value) {
        if (value == null) {
            return null;
        }
        switch (value.getType().toJsonString()) {
            case ValueType.JSON_BOOL:
                return value.getBool();
            case ValueType.JSON_NUMBER:
                return value.getNumber();
            case ValueType.JSON_STRING:
                return value.getString();
            case ValueType.JSON_MAP:
                return value.getMap();
            case ValueType.JSON_ARRAY:
                return value.getArray();
            case ValueType.JSON_BINARY:
                return value.getBinary();
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
