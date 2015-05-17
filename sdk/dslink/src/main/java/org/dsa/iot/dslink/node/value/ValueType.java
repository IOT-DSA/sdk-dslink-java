package org.dsa.iot.dslink.node.value;

import org.dsa.iot.dslink.util.StringUtils;

import java.util.Set;

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

    private final String jsonName;

    private ValueType(String jsonName) {
        this.jsonName = jsonName;
    }

    /**
     * @return Type of value that is suitable for JSON consumption.
     */
    public String toJsonString() {
        return jsonName;
    }

    public static ValueType makeEnum(Set<String> enums) {
        String built = "enum[" + StringUtils.join(enums, ",") + "]";
        return new ValueType(built);
    }

    /**
     * @param type Type to convert
     * @return Converted type
     */
    public static ValueType toEnum(String type) {
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
                if (type.startsWith(JSON_ENUM + "[") && type.endsWith("]")) {
                    return new ValueType(type);
                }
                throw new RuntimeException("Unknown type: " + type);
        }
    }
}
