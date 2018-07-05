package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.util.TimeUtils;
import org.dsa.iot.dslink.util.UrlBase64;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Arrays;

/**
 * Common class for handling values. It is always recommended to check the type
 * before using a getter.
 *
 * @author Samuel Grenier
 */
public class Value {

    private ValueType type;
    private boolean immutable;
    private boolean serializable = true;

    private long tsDate = -1;
    private String tsFormatted;

    private Number number;
    private Boolean bool;
    private String string;
    private JsonObject map;
    private JsonArray array;
    private byte[] binary;

    /**
     * Creates a value with an initial type of binary.
     *
     * @param b Initial binary type.
     */
    public Value(byte[] b) {
        set(b);
    }

    /**
     * Creates a value with an initial type of a binary.
     *
     * @param b Initial binary data.
     * @param time Initial time to set.
     */
    public Value(byte[] b, String time) {
        set(b, time);
    }

    /**
     * Creates a value with an initial type of a number.
     *
     * @param n Initial number to set.
     */
    public Value(Number n) {
        set(n);
    }

    /**
     * @param n Initial number to set.
     * @param time Initial time to set.
     */
    public Value(Number n, String time) {
        set(n, time);
    }

    /**
     * Creates a value with an initial type of a boolean.
     *
     * @param b Initial boolean to set.
     */
    public Value(Boolean b) {
        set(b);
    }

    /**
     * @param b Initial boolean to set.
     * @param time Initial time to set.
     */
    public Value(Boolean b, String time) {
        set(b, time);
    }

    /**
     * Creates a value with an initial type of a string.
     *
     * @param s Initial string to set.
     */
    public Value(String s) {
        set(s);
    }

    /**
     * @param s Initial string to set.
     * @param time Initial time to set.
     */
    public Value(String s, String time) {
        set(s, time);
    }

    /**
     * Creates a value with an initial type of a JSON object.
     *
     * @param o Initial JSON object to set.
     */
    public Value(JsonObject o) {
        set(o);
    }

    /**
     * @param o Initial JSON object to set.
     * @param time Initial time to set.
     */
    public Value(JsonObject o, String time) {
        set(o, time);
    }

    /**
     * Creates a value with an initial type of a JSON array.
     *
     * @param a Initial JSON array to set.
     */
    public Value(JsonArray a) {
        set(a);
    }

    /**
     * @param a Initial JSON array to set.
     * @param time Initial time to set.
     */
    public Value(JsonArray a, String time) {
        set(a, time);
    }

    /**
     * @param b Binary data to set.
     */
    public void set(byte[] b) {
        set(b, null);
    }

    public void set(byte[] b, String time) {
        set(ValueType.BINARY, b, null, null, null, null, null, time);
    }

    /**
     * @param n Number to set
     */
    public void set(Number n) {
        set(n, null);
    }

    /**
     * @param n Number to set.
     * @param time Initial time to set.
     */
    public void set(Number n, String time) {
        set(ValueType.NUMBER, null, n, null, null, null, null, time);
    }

    /**
     * @param b Boolean to set
     */
    public void set(Boolean b) {
        set(b, null);
    }

    /**
     * @param b Boolean to set.
     * @param time Initial time to set.
     */
    public void set(Boolean b, String time) {
        set(ValueType.BOOL, null, null, b, null, null, null, time);
    }

    /**
     * @param s String to set.
     */
    public void set(String s) {
        set(s, null);
    }

    /**
     * @param s String to set.
     * @param time Initial time to set.
     */
    public void set(String s, String time) {
        if (s != null) {
            String start = "\u001Bbytes:";
            if (s.startsWith(start)) {
                s = s.substring(start.length(), s.length());
                byte[] bytes = UrlBase64.decode(s);
                set(bytes, time);
                return;
            }
        }
        set(ValueType.STRING, null, null, null, s, null, null, time);
    }

    /**
     * @param object JSON object to set
     */
    public void set(JsonObject object) {
        set(object, null);
    }

    /**
     * @param object JSON object to set.
     * @param time Initial time to set.
     */
    public void set(JsonObject object, String time) {
        set(ValueType.MAP, null, null, null, null, null, object, time);
    }

    /**
     * @param array JSON array to set
     */
    public void set(JsonArray array) {
        set(array, null);
    }

    /**
     * @param array JSON array to set.
     * @param time Initial time to set.
     */
    public void set(JsonArray array, String time) {
        set(ValueType.ARRAY, null, null, null, null, array, null, time);
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
    private void set(ValueType type, byte[] binary, Number n, Boolean b,
                     String s, JsonArray a, JsonObject o, String time) {
        checkImmutable();
        this.type = type;
        if (time == null) {
            setTime(System.currentTimeMillis());
        } else {
            this.tsFormatted = time;
        }

        this.binary = binary == null ? null : binary.clone();
        this.number = n;
        this.bool = b;
        this.string = s;
        this.array = a;
        this.map = o;
    }

    /**
     * Sets the time of the value.
     *
     * @param ms Time to set.
     */
    public void setTime(long ms) {
        checkImmutable();
        this.tsDate = ms;
        this.tsFormatted = null;
    }

    /**
     * This attribute only takes effect for serialization of {@link Node}'s.
     *
     * @param serializable Whether this value is allowed to be serialized.
     */
    public void setSerializable(boolean serializable) {
        checkImmutable();
        this.serializable = serializable;
    }

    public boolean isSerializable() {
        return serializable;
    }

    /**
     * Gets the internal type of the value.
     *
     * @return Type of the value
     */
    public ValueType getType() {
        return type;
    }

    /**
     * Time stamp is always updated when the value is created or updated
     * with a new value.
     *
     * @return The formatted time this value was set or created.
     */
    public String getTimeStamp() {
        if (tsFormatted == null) {
            tsFormatted = TimeUtils.format(getTime());
        }
        return tsFormatted;
    }

    /**
     * Time stamp is always
     *
     * @return The raw date this value was set or created.
     */
    public long getTime() {
        if (tsDate == -1) {
            tsDate = TimeUtils.parseTz(tsFormatted).getTime();
        }
        return tsDate;
    }

    public byte[] getBinary() {
        return binary == null ? null : binary.clone();
    }

    /**
     * @return Number of the value
     */
    public Number getNumber() {
        return number;
    }

    /**
     * @return Boolean of the value
     */
    public Boolean getBool() {
        return bool;
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
        return map;
    }

    /**
     * @return JSON array of the value
     */
    public JsonArray getArray() {
        return array;
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
        switch (type.toJsonString()) {
            case ValueType.JSON_NUMBER:
                return String.valueOf(number);
            case ValueType.JSON_BOOL:
                return String.valueOf(bool);
            case ValueType.JSON_STRING:
                return String.valueOf(string);
            case ValueType.JSON_MAP:
                return String.valueOf(map);
            case ValueType.JSON_ARRAY:
                return String.valueOf(array);
            case ValueType.JSON_BINARY:
                return Arrays.toString(binary);
            default:
                throw new RuntimeException("Unhandled type: " + type);
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if (o instanceof Value) {
            Value value = (Value) o;
            String compare = getType().toJsonString();
            if (value.getType().toJsonString().equals(compare)) {
                switch (compare) {
                    case ValueType.JSON_NUMBER:
                        equal = objectEquals(number, value.number);
                        break;
                    case ValueType.JSON_TIME:
                    case ValueType.JSON_STRING:
                        equal = objectEquals(string, value.string);
                        break;
                    case ValueType.JSON_BOOL:
                        equal = objectEquals(bool, value.bool);
                        break;
                    case ValueType.JSON_MAP:
                        equal = objectEquals(map, value.map);
                        break;
                    case ValueType.JSON_ARRAY:
                        equal = objectEquals(array, value.array);
                        break;
                    default:
                        String err = "Bad type: " + getType();
                        throw new RuntimeException(err);
                }
            }
        }
        return equal;
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + (getType().hashCode());
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
}
