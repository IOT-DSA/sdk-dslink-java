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
    private EditorType editorType;

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
     * Sets the editor type of the parameter, used for parameters in an
     * action.
     *
     * @param type Editor type to set.
     * @return This object for daisy chaining.
     */
    public Parameter setEditorType(EditorType type) {
        if (!this.type.compare(ValueType.STRING)) {
            throw new RuntimeException("Parameter must be of type string");
        }
        this.editorType = type;
        return this;
    }

    /**
     * @return Editor type this parameter uses.
     */
    public EditorType getEditorType() {
        return editorType;
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
