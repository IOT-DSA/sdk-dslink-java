package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.util.StringUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

/**
 * Common class for handling values. It is always recommended to check the type
 * before using a getter.
 *
 * @author Samuel Grenier
 */
public class Value {

    private static final DateFormat FORMAT;
    private static final Object LOCK;
    private static final String TIMEZONE;

    private final ValueType visibleType;
    private ValueType internalType;

    private boolean immutable;
    private String ts;

    private Number number;
    private Boolean bool;
    private String string;
    private JsonObject map;
    private JsonArray array;
    private Set<String> enums;

    /**
     * Creates a value with an initial type of a number. The value type
     * cannot be changed through the setter.
     *
     * @param n Initial number to set.
     */
    public Value(Number n) {
        this(n, false);
    }

    /**
     * Creates a value with an initial type of a number. If the value is
     * dynamic then the value can be changed to anything.
     *
     * @param n Initial number to set.
     * @param dynamic Whether the value is dynamic or not.
     */
    public Value(Number n, boolean dynamic) {
        this.visibleType = dynamic ? ValueType.DYNAMIC : ValueType.NUMBER;
        set(n);
    }

    /**
     * Creates a value with an initial type of a boolean.
     *
     * @param b Initial boolean to set.
     */
    public Value(Boolean b) {
        this(b, false);
    }

    /**
     * Creates a value with an initial type of a boolean. If the value is
     * dynamic then the value can be changed to anything.
     *
     * @param b Initial boolean to set.
     * @param dynamic Whether the value is dynamic or not.
     */
    public Value(Boolean b, boolean dynamic) {
        this.visibleType = dynamic ? ValueType.DYNAMIC : ValueType.BOOL;
        set(b);
    }

    /**
     * Creates a value with an initial type of a string.
     *
     * @param s Initial string to set.
     */
    public Value(String s) {
        this(s, false);
    }

    /**
     * Creates a value with an initial type of a string. If the value is
     * dynamic then the value can be changed to anything.
     *
     * @param s Initial string to set.
     * @param dynamic Whether the value is dynamic or not.
     */
    public Value(String s, boolean dynamic) {
        this.visibleType = dynamic ? ValueType.DYNAMIC : ValueType.STRING;
        set(s);
    }

    /**
     * Creates a value with an initial type of a JSON object.
     *
     * @param o Initial JSON object to set.
     */
    public Value(JsonObject o) {
        this(o, false);
    }

    /**
     * Creates a value with an initial type of a JSON object. If the value is
     * dynamic then the value can be set to anything.
     *
     * @param o Initial JSON object to set.
     * @param dynamic Whether the value is dynamic or not.
     */
    public Value(JsonObject o, boolean dynamic) {
        this.visibleType = dynamic ? ValueType.DYNAMIC : ValueType.MAP;
        set(o);
    }

    /**
     * Creates a value with an initial type of a JSON array.
     *
     * @param a Initial JSON array to set.
     */
    public Value(JsonArray a) {
        this(a, false);
    }

    /**
     * Creates a value with an initial type of a JSON array. If the value is
     * dynamic then the value can be set to anything.
     *
     * @param a Initial JSON array to set.
     * @param dynamic Whether the value is dynamic or not.
     */
    public Value(JsonArray a, boolean dynamic) {
        this.visibleType = dynamic ? ValueType.DYNAMIC : ValueType.ARRAY;
        set(a);
    }

    /**
     * Creates a value with an initial type of an enum.
     *
     * @param e Initial enumerations to set.
     */
    public Value(Set<String> e) {
        this(e, false);
    }

    /**
     * Creates a value with an initial type of an enum. If the value is dynamic
     * then the value can later be set to anything.
     *
     * @param e Initial enumerations to set.
     * @param dynamic Whether the value is dynamic or not.
     */
    public Value(Set<String> e, boolean dynamic) {
        this.visibleType = dynamic ? ValueType.DYNAMIC : ValueType.ENUM;
        set(e);
    }

    /**
     * @param n Number to set
     */
    public void set(Number n) {
        set(ValueType.NUMBER, n, null, null, null, null, null);
    }

    /**
     * @param b Boolean to set
     */
    public void set(Boolean b) {
        set(ValueType.BOOL, null, b, null, null, null, null);
    }

    /**
     * @param s String to set
     */
    public void set(String s) {
        set(ValueType.STRING, null, null, s, null, null, null);
    }

    /**
     * @param object JSON object to set
     */
    public void set(JsonObject object) {
        set(ValueType.MAP, null, null, null, null, object, null);
    }

    /**
     * @param array JSON array to set
     */
    public void set(JsonArray array) {
        set(ValueType.ARRAY, null, null, null, array, null, null);
    }

    /**
     * @param enums Enumerations to set.
     */
    public void set(Set<String> enums) {
        set(ValueType.ENUM, null, null, null, null, null, enums);
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
     * @param e    Enumerations to set, or null
     */
    private void set(ValueType type, Number n, Boolean b, String s,
                     JsonArray a, JsonObject o, Set<String> e) {
        checkImmutable();
        if (!(visibleType == ValueType.DYNAMIC
                || internalType == null || internalType == type)) {
            throw new RuntimeException("Value is not dynamic");
        }
        this.internalType = type;
        synchronized (LOCK) {
            this.ts = FORMAT.format(new Date()) + TIMEZONE;
        }

        this.number = n;
        this.bool = b;
        this.string = s;
        this.enums = e;
        this.array = a != null ? a.copy() : null;
        this.map = o != null ? o.copy() : null;
    }

    /**
     * Getting the type will allow you use to use the recommended getter
     * that this value is used for.
     *
     * @return Type of the value
     */
    @Deprecated
    public ValueType getType() {
        return getInternalType();
    }

    /**
     * Retrieves the real type of the value that should be exposed to the
     * outside world.
     *
     * @return The real type of the value.
     */
    public ValueType getVisibleType() {
        return visibleType;
    }

    /**
     * Retrieves the internal type of the value that the value is currently
     * holding. The internal type can never be {@link ValueType#DYNAMIC}.
     *
     * @return The internal type of the value.
     */
    public ValueType getInternalType() {
        return internalType;
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

    public Set<String> getEnums() {
        return enums == null ? null : Collections.unmodifiableSet(enums);
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
        switch (internalType) {
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
            case ENUM:
                return "enum[" + StringUtils.join(enums, ",") + "]";
            default:
                throw new RuntimeException("Unhandled type: " + internalType);
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if (o instanceof Value) {
            Value value = (Value) o;
            if (value.getInternalType().equals(getInternalType())) {
                switch (value.getInternalType()) {
                    case NUMBER:
                        equal = objectEquals(number, value.number);
                        break;
                    case TIME:
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
                    case ENUM:
                        equal = objectEquals(enums, value.enums);
                        break;
                    default:
                        String err = "Bad internal type: " + getInternalType();
                        throw new RuntimeException(err);
                }
            }

            if (equal) {
                equal = value.getVisibleType() == getVisibleType();
            }
        }
        return equal;
    }

    @Override
    public int hashCode() {
        int result = getInternalType().hashCode();
        result = 31 * result + (getVisibleType().hashCode());
        result = 31 * result + (getNumber() != null ? getNumber().hashCode() : 0);
        result = 31 * result + (getBool() != null ? getBool().hashCode() : 0);
        result = 31 * result + (getString() != null ? getString().hashCode() : 0);
        result = 31 * result + (getMap() != null ? getMap().hashCode() : 0);
        result = 31 * result + (getArray() != null ? getArray().hashCode() : 0);
        result = 31 * result + (getEnums() != null ? getEnums().hashCode() : 0);
        return result;
    }

    private boolean objectEquals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    static {
        long currentTime = new Date().getTime();
        int offset = TimeZone.getDefault().getOffset(currentTime) / (1000 * 60);
        String s = "+";
        if (offset < 0) {
            offset = -offset;
            s = "-";
        }
        int hh = offset / 60;
        int mm = offset % 60;
        TIMEZONE = s + (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
        FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
        LOCK = new Object();
    }
}
