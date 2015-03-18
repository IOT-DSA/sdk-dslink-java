package org.dsa.iot.dslink.node.value;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Common class for handling values. It is always recommended to check the type
 * before using a getter.
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

    /**
     * Creates a value with an initial type of a number.
     * @param n Number to set
     */
    public Value(Number n) {
        set(n);
    }

    /**
     * Creates a value with an initial type of a boolean.
     * @param b Boolean to set
     */
    public Value(Boolean b) {
        set(b);
    }

    /**
     * Creates a value with an initial type of a string.
     * @param s String to set
     */
    public Value(String s) {
        set(s);
    }

    /**
     * Creates a value with an initial type of a JSON object.
     * @param o JSON object to set
     */
    public Value(JsonObject o) {
        set(o);
    }

    /**
     * Creates a value with an initial type of a JSON object.
     * @param a JSON array to set
     */
    public Value(JsonArray a) {
        set(a);
    }

    /**
     * @param n Number to set
     */
    public void set(Number n) {
        set(ValueType.NUMBER, n, null, null, null, null);
    }

    /**
     * @param b Boolean to set
     */
    public void set(Boolean b) {
        set(ValueType.BOOL, null, b, null, null, null);
    }

    /**
     * @param s String to set
     */
    public void set(String s) {
        set(ValueType.STRING, null, null, s, null, null);
    }

    /**
     * @param array JSON array to set
     */
    public void set(JsonArray array) {
        set(ValueType.ARRAY, null, null, null, array, null);
    }

    /**
     * @param object JSON object to set
     */
    public void set(JsonObject object) {
        set(ValueType.MAP, null, null, null, null, object);
    }

    /**
     * Sets the value to the new value and type. However immutability is
     * checked. If this value is declared as immutable, an exception will be
     * thrown.
     * @param type New type for the value
     * @param n Number to set, or null
     * @param b Boolean to set, or null
     * @param s String to set, or null
     * @param a JSON array to set, or null
     * @param o JSON object to set, or null
     */
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

    /**
     * Getting the type will allow you use to use the recommended getter
     * that this value is used for.
     * @return Type of the value
     */
    public ValueType getType() {
        return type;
    }

    /**
     * @return Boolean of the value
     */
    public Boolean getBool() {
        return bool;
    }

    /**
     * @return Number of the value
     */
    public Number getNumber() {
        return number;
    }

    /**
     * @return String of the value
     */
    public String getString() {
        return string;
    }

    /**
     * @return JSON object of the value
     */
    public JsonObject getMap() {
        return map == null ? null : map.copy();
    }

    /**
     * @return JSON array of the value
     */
    public JsonArray getArray() {
        return array == null ? null : array.copy();
    }

    /**
     * Declares this value as immutable. Using setters will throw an exception
     * whenever they are called.
     */
    public void setImmutable() {
        immutable = true;
    }

    /**
     * @return Whether this value is immutable or not.
     */
    public boolean isImmutable() {
        return immutable;
    }

    /**
     * Standard immutability check throwing an exception if the value is
     * declared as immutable.
     */
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

    /**
     * Used for printing out values to the console.
     * @return Printable string
     */
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
