package org.dsa.iot.dslink.node.value;

/**
 * Type of the value
 *
 * @author Samuel Grenier
 * @see Value
 */
public enum ValueType {

    NUMBER,
    STRING,
    BOOL,
    MAP,
    ARRAY,
    TIME;

    /**
     * @return Type of value that is suitable for JSON consumption.
     */
    public String toJsonString() {
        return this.name().toLowerCase();
    }

    /**
     * @param type Type to convert
     * @return Converted type
     */
    public static ValueType toEnum(String type) {
        switch (type) {
            case "number":
                return NUMBER;
            case "string":
                return STRING;
            case "bool":
                return BOOL;
            case "map":
                return MAP;
            case "array":
                return ARRAY;
            case "time":
                return TIME;
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }
}
