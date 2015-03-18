package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.value.ValueType;

/**
 * @author Samuel Grenier
 */
public class Parameter {

    private final String name;
    private final ValueType type;

    public Parameter(String name, ValueType type) {
        if (name == null)
            throw new NullPointerException("name");
        else if (type == null)
            throw new NullPointerException("type");
        this.name = name;
        this.type = type;
    }

    /**
     * @return Name of the parameter
     */
    public String getName() {
        return name;
    }

    /**
     * @return Value type of the parameter
     */
    public ValueType getType() {
        return type;
    }
}