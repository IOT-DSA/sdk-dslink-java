package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

/**
 * @author Samuel Grenier
 */
public class Parameter {

    private final String name;
    private final ValueType type;
    private final Value defVal;

    public Parameter(String name, ValueType type) {
        this(name, type, null);
    }

    /**
     *
     * @param name Name of the parameter
     * @param type Type of the parameter
     * @param def Default value of the parameter, if this parameter is used as
     *            as an action parameter rather than an action result.
     */
    public Parameter(String name, ValueType type, Value def) {
        if (name == null)
            throw new NullPointerException("name");
        else if (type == null)
            throw new NullPointerException("type");
        this.name = name;
        this.type = type;
        this.defVal = def;
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

    /**
     * @return Default value of the parameter
     */
    public Value getDefault() {
        return defVal;
    }
}
