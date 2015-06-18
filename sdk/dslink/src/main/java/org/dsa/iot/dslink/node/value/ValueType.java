package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.util.StringUtils;

import java.util.*;

/**
 * Type of the value
 *
 * @author Samuel Grenier
 * @see Value
 */
public final class ValueType {

    public static final String JSON_NUMBER = "number";
    public static final String JSON_STRING = "string";
    public static final String JSON_BOOL = "bool";
    public static final String JSON_MAP = "map";
    public static final String JSON_ARRAY = "array";
    public static final String JSON_TIME = "time";
    public static final String JSON_ENUM = "enum";
    public static final String JSON_DYNAMIC = "dynamic";

    public static final ValueType NUMBER = new ValueType(JSON_NUMBER);
    public static final ValueType STRING = new ValueType(JSON_STRING);
    public static final ValueType BOOL = new ValueType(JSON_BOOL);
    public static final ValueType MAP = new ValueType(JSON_MAP);
    public static final ValueType ARRAY = new ValueType(JSON_ARRAY);
    public static final ValueType TIME = new ValueType(JSON_TIME);
    public static final ValueType DYNAMIC = new ValueType(JSON_DYNAMIC);
    public static final ValueType ENUM = new ValueType(JSON_ENUM);

    private final String rawName;
    private final String builtName;
    private final Set<String> enums;

    private ValueType(String jsonName) {
        this(jsonName, jsonName);
    }

    private ValueType(String rawName, String builtName) {
        this(rawName, builtName, null);
    }

    private ValueType(String rawName, String builtName, Set<String> enums) {
        this.rawName = rawName;
        this.builtName = builtName;
        this.enums = enums;
    }

    private ValueType(Set<String> enums) {
        this(JSON_ENUM, "enum[" + StringUtils.join(enums, ",") + "]", enums);
    }

    /**
     * @return Type of value that is suitable for JSON consumption.
     */
    public String toJsonString() {
        return builtName;
    }

    /**
     * Compares whether the 2 raw JSON types are equal or not. This is
     * necessary when comparing if a type is an enum or not.
     *
     * @param other Other type to compare
     * @return Whether the JSON types are equal or not.
     */
    public boolean compare(ValueType other) {
        return this == other || this.rawName.equals(other.rawName);
    }

    /**
     * @return The enums this value type represents
     */
    public Set<String> getEnums() {
        return enums != null ? Collections.unmodifiableSet(enums) : null;
    }

    public static ValueType makeEnum(String... enums) {
        Set<String> e = new LinkedHashSet<>(Arrays.asList(enums));
        return makeEnum(e);
    }

    public static ValueType makeEnum(Set<String> enums) {
        return new ValueType(enums);
    }

    public static ValueType makeBool(String true_, String false_) {
        return new ValueType(JSON_BOOL, "bool[" + false_ + "," + true_ + "]", null);
    }

    /**
     * @param type Type to convert
     * @return Converted type
     */
    public static ValueType toValueType(String type) {
        switch (type) {
            case JSON_NUMBER:
                return NUMBER;
            case JSON_STRING:
                return STRING;
            case JSON_BOOL:
                return BOOL;
            case JSON_MAP:
                return MAP;
            case JSON_ARRAY:
                return ARRAY;
            case JSON_TIME:
                return TIME;
            case JSON_DYNAMIC:
                return DYNAMIC;
            default:
                if (type.startsWith(JSON_BOOL + "[") && type.endsWith("]")) {
                    return new ValueType(JSON_BOOL, type);
                } else if (type.startsWith(JSON_ENUM + "[") && type.endsWith("]")) {
                    type = type.substring(JSON_ENUM.length() + 1);
                    type = type.substring(0, type.length() - 1);
                    String[] split = type.split(",");
                    List<String> list = Arrays.asList(split);
                    Set<String> enums = new LinkedHashSet<>(list);
                    return new ValueType(enums);
                }
                throw new RuntimeException("Unknown type: " + type);
        }
    }
}
