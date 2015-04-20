package org.dsa.iot.dslink.node.value;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Common class for handling values. It is always recommended to check the type
 * before using a getter.
 *
 * @author Samuel Grenier
 */
public class Value {

    private static final DateFormat FORMAT;
    private static final Object LOCK;

    private boolean immutable;
    private ValueType type;
    private String ts;

    private Number number;
    private Boolean bool;
    private String string;
    private JsonObject map;
    private JsonArray array;

    /**
     * Creates a value with an initial type of a number.
     *
     * @param n Number to set
     */
    public Value(Number n) {
        set(n);
    }

    /**
     * Creates a value with an initial type of a boolean.
     *
     * @param b Boolean to set
     */
    public Value(Boolean b) {
        set(b);
    }

    /**
     * Creates a value with an initial type of a string.
     *
     * @param s String to set
     */
    public Value(String s) {
        set(s);
    }

    /**
     * Creates a value with an initial type of a JSON object.
     *
     * @param o JSON object to set
     */
    public Value(JsonObject o) {
        set(o);
    }

    /**
     * Creates a value with an initial type of a JSON object.
     *
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
     *
     * @param type New type for the value
     * @param n    Number to set, or null
     * @param b    Boolean to set, or null
     * @param s    String to set, or null
     * @param a    JSON array to set, or null
     * @param o    JSON object to set, or null
     */
    private void set(ValueType type, Number n, Boolean b, String s,
                     JsonArray a, JsonObject o) {
        checkImmutable();
        this.type = type;
        synchronized (LOCK) {
            this.ts = FORMAT.format(new Date());
        }

        this.number = n;
        this.bool = b;
        this.string = s;
        this.array = a != null ? a.copy() : null;
        this.map = o != null ? o.copy() : null;
    }

    /**
     * Getting the type will allow you use to use the recommended getter
     * that this value is used for.
     *
     * @return Type of the value
     */
    public ValueType getType() {
        return type;
    }

    /**
     * Time stamp is always updates when the value is created or updated
     * with a new value.
     *
     * @return The time this value was set
     */
    public String getTimeStamp() {
        return ts;
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
            case MAP:
                return map.encode();
            case ARRAY:
                return array.encode();
            default:
                throw new RuntimeException("Unhandled type: " + type);
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if (o instanceof Value) {
            Value value = (Value) o;
            if (value.type.equals(getType())) {
                switch (type) {
                    case NUMBER:
                        equal = objectEquals(number, value.number);
                        break;
                    case STRING:
                        equal = objectEquals(string, value.string);
                        break;
                    case BOOL:
                        equal = objectEquals(bool, value.bool);
                        break;
                    case MAP:
                        equal = objectEquals(map, value.map);
                        break;
                    case ARRAY:
                        equal = objectEquals(array, value.array);
                        break;
                    case TIME:
                        equal = objectEquals(string, value.string);
                }
            }
        }
        return equal;
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + (getNumber() != null ? getNumber().hashCode() : 0);
        result = 31 * result + (getBool() != null ? getBool().hashCode() : 0);
        result = 31 * result + (getString() != null ? getString().hashCode() : 0);
        result = 31 * result + (getMap() != null ? getMap().hashCode() : 0);
        result = 31 * result + (getArray() != null ? getArray().hashCode() : 0);
        return result;
    }

    private boolean objectEquals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    static {
        FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        LOCK = new Object();
    }
}
