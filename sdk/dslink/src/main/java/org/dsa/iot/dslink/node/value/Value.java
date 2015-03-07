package org.dsa.iot.dslink.node.value;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
@EqualsAndHashCode(exclude = "immutable")
public class Value {

    private boolean immutable;
    private ValueType type;

    private Integer integer;
    private Boolean bool;
    private String string;
    private JsonObject map;
    private JsonArray array;

    public Value(Integer i) {
        set(i);
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

    public void set(Integer i) {
        set(ValueType.NUMBER, i, null, null, null, null);
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

    private void set(ValueType type, Integer i, Boolean b, String s,
            JsonArray a, JsonObject o) {
        checkImmutable();
        this.type = type;
        this.integer = i;
        this.bool = b;
        this.string = s;
        this.array = a != null ? a.copy() : null;
        this.map = o != null ? o.copy() : null;
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

    private void checkImmutable() {
        if (isImmutable()) {
            throw new IllegalStateException(
                    "Attempting to modify immutable value");
        }
    }

    @Override
    public String toString() {
        switch (type) {
        case NUMBER:
            return integer.toString();
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
