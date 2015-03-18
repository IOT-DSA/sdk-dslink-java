package org.dsa.iot.dslink.node.value;

/**
 * Type of the value
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
    ARRAY
}
