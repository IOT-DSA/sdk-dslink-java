package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.util.*;
import java.util.*;

/**
 * Type of the value
 *
 * @author Samuel Grenier
 * @see Value
 */
public final class ValueType {

    public static final String JSON_NUMBER = "number";
    public static final String JSON_INT = "int";
    public static final String JSON_STRING = "string";
    public static final String JSON_BOOL = "bool";
    public static final String JSON_MAP = "map";
    public static final String JSON_ARRAY = "array";
    public static final String JSON_TIME = "time";
    public static final String JSON_ENUM = "enum";
    public static final String JSON_BINARY = "binary";
    public static final String JSON_DYNAMIC = "dynamic";

    public static final ValueType NUMBER = new ValueType(JSON_NUMBER);
    public static final ValueType STRING = new ValueType(JSON_STRING);
    public static final ValueType BOOL = new ValueType(JSON_BOOL);
    public static final ValueType MAP = new ValueType(JSON_MAP);
    public static final ValueType ARRAY = new ValueType(JSON_ARRAY);
    public static final ValueType TIME = new ValueType(JSON_TIME);
    public static final ValueType DYNAMIC = new ValueType(JSON_DYNAMIC);
    public static final ValueType ENUM = new ValueType(JSON_ENUM);
    public static final ValueType BINARY = new ValueType(JSON_BINARY);

    private final String rawName;
    private final String builtName;
    private final Collection<String> enums;

    private ValueType(String jsonName) {
        this(jsonName, jsonName);
    }

    private ValueType(String rawName, String builtName) {
        this(rawName, builtName, null);
    }

    private ValueType(String rawName, String builtName, Collection<String> enums) {
        this.rawName = rawName;
        this.builtName = builtName;
        this.enums = enums;
    }

    private ValueType(Collection<String> enums) {
        this(JSON_ENUM, "enum[" + StringUtils.join(enums, true, ",") + "]", enums);
    }

    /**
     * @return Type of value that is suitable for JSON consumption.
     */
    public String toJsonString() {
        return builtName;
    }

    /**
     * @return Raw value type that may not be suitable for JSON consumption.
     */
    public String getRawName() {
        return rawName;
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
    public Collection<String> getEnums() {
        return enums != null ? Collections.unmodifiableCollection(enums) : null;
    }

    public static ValueType makeEnum(String... enums) {
        return makeEnum(Arrays.asList(enums));
    }

    public static ValueType makeEnum(Collection<String> enums) {
        return new ValueType(enums);
    }

    public static ValueType makeBool(String true_, String false_) {
        true_ = StringUtils.encodeName(true_);
        false_ = StringUtils.encodeName(false_);
        String contents = false_ + "," + true_;
        return new ValueType(JSON_BOOL, "bool[" + contents + "]", null);
    }

    /**
     * @param type Type to convert
     * @return Converted type
     */
    public static ValueType toValueType(String type) {
        if (type == null) {
            return ValueType.DYNAMIC;
        }

        switch (type) {
            case JSON_NUMBER:
            case JSON_INT:
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
            case JSON_BINARY:
                return BINARY;
            case JSON_ENUM:
                return new ValueType(new ArrayList<String>(0));
            default:
                if (type.startsWith(JSON_BOOL + "[") && type.endsWith("]")) {
                    return new ValueType(JSON_BOOL, type);
                } else if (type.startsWith(JSON_ENUM + "[") && type.endsWith("]")) {
                    type = type.substring(JSON_ENUM.length() + 1);
                    type = type.substring(0, type.length() - 1);
                    String[] split = StringUtils.split(type, true, ",");
                    return new ValueType(Arrays.asList(split));
                }
                return DYNAMIC;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ValueType) {
            return ((ValueType) other).toJsonString().equals(toJsonString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * toJsonString().hashCode();
    }
}
