package org.dsa.iot.dslink.node.value;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Common class for handling values
 * @author Samuel Grenier
 */
public class Value {

    private boolean immutable;
    private ValueType type;

    private Number number;
    private Boolean bool;
    private String string;
    private JsonObject map;
    private JsonArray array;

    public Value(Number n) {
        set(n);
    }

    public Value(Boolean b) {
        set(b);
    }

    public Value(String s) {
        set(s);
    }

    public Value(JsonObject o) {
        set(o);
    }

    public Value(JsonArray a) {
        set(a);
    }

    public void set(Number n) {
        set(ValueType.NUMBER, n, null, null, null, null);
    }

    public void set(Boolean b) {
        set(ValueType.BOOL, null, b, null, null, null);
    }

    public void set(String s) {
        set(ValueType.STRING, null, null, s, null, null);
    }

    public void set(JsonArray array) {
        set(ValueType.ARRAY, null, null, null, array, null);
    }

    public void set(JsonObject object) {
        set(ValueType.MAP, null, null, null, null, object);
    }

    private void set(ValueType type, Number n, Boolean b, String s,
                                            JsonArray a, JsonObject o) {
        checkImmutable();
        this.type = type;
        this.number = n;
        this.bool = b;
        this.string = s;
        this.array = a != null ? a.copy() : null;
        this.map = o != null ? o.copy() : null;
    }

    public ValueType getType() {
        return type;
    }

    public Boolean getBool() {
        return bool;
    }

    public Number getNumber() {
        return number;
    }

    public String getString() {
        return string;
    }

    public JsonObject getMap() {
        return map == null ? null : map.copy();
    }

    public JsonArray getArray() {
        return array == null ? null : array.copy();
    }

    public void setImmutable() {
        immutable = true;
    }

    public boolean isImmutable() {
        return immutable;
    }

    private void checkImmutable() {
        if (isImmutable()) {
            String err = "Attempting to modify immutable value";
            throw new IllegalStateException(err);
        }
    }

    @Override
    public String toString() {
        switch (type) {
        case NUMBER:
            return number.toString();
        case BOOL:
            return bool.toString();
        case STRING:
            return string;
        default:
            throw new RuntimeException("Unhandled type: " + type);
        }
    }

    public String toDebugString() {
        switch (type) {
            case MAP:
                return map.encode();
            case ARRAY:
                return array.encode();
            default:
                return toString();
        }
    }
}
