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
}
