package org.dsa.iot.dslink.node.value;

/**
 * Type of the value
 *
 * @author Samuel Grenier
 * @see Value
 */
public enum ValueType {

    /* Json values */
    NUMBER,
    STRING,
    BOOL,

    /* Internal values */
    MAP,
    ARRAY;

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
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }
}
