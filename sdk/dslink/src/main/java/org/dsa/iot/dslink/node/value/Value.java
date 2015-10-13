package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Common class for handling values. It is always recommended to check the type
 * before using a getter.
 *
 * @author Samuel Grenier
 */
public class Value {

    private static final ThreadLocal<DateFormat> FORMAT_TIME_ZONE;
    private static final ThreadLocal<DateFormat> FORMAT;
    private static final String TIME_ZONE;

    private ValueType type;
    private boolean immutable;

    private Date tsDate;
    private String tsFormatted;

    private Number number;
    private Boolean bool;
    private String string;
    private JsonObject map;
    private JsonArray array;

    /**
     * Creates a value with an initial type of a number. The value type
     * cannot be changed through the setter.
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
        set(ValueType.NUMBER, n, null, null, null, null, time);
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
        set(ValueType.BOOL, null, b, null, null, null, time);
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
        set(ValueType.STRING, null, null, s, null, null, time);
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
        set(ValueType.MAP, null, null, null, null, object, time);
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
        set(ValueType.ARRAY, null, null, null, array, null, time);
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
                     JsonArray a, JsonObject o, String time) {
        checkImmutable();
        this.type = type;
        if (time == null) {
            setTime(System.currentTimeMillis());
        } else {
            if (time.matches(".+[+|-]\\d+:\\d+")) {
                StringBuilder builder = new StringBuilder(time);
                builder.deleteCharAt(time.lastIndexOf(":"));
                time = builder.toString();
            } else {
                time += TIME_ZONE;
            }
            this.tsFormatted = time;
        }

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
        this.tsDate = new Date(ms);
        this.tsFormatted = null;
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
            tsFormatted = FORMAT.get().format(getDate()) + TIME_ZONE;
        }
        return tsFormatted;
    }

    /**
     * Time stamp is always
     *
     * @return The raw date this value was set or created.
     */
    public Date getDate() {
        if (tsDate == null) {
            try {
                this.tsDate = FORMAT_TIME_ZONE.get().parse(tsFormatted);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return new Date(tsDate.getTime());
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
                return number.toString();
            case ValueType.JSON_BOOL:
                return bool.toString();
            case ValueType.JSON_STRING:
                return string;
            case ValueType.JSON_MAP:
                return map.encode();
            case ValueType.JSON_ARRAY:
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
        TIME_ZONE = s + (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
        FORMAT = new ThreadLocal<DateFormat>() {
            @Override
            public DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            }
        };
        FORMAT_TIME_ZONE = new ThreadLocal<DateFormat>() {
            @Override
            public DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            }
        };
    }
}
