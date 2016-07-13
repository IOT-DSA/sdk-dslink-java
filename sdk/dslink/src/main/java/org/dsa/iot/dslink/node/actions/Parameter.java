package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class Parameter {

    private final String name;
    private ValueType type;
    private Value defVal;

    private EditorType editorType;
    private String description;
    private String placeHolder;
    private JsonObject meta;

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
     *
     * @param type Value type of the parameter.
     * @return THis object for daisy chaining.
     */
    public Parameter setValueType(ValueType type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.type = type;
        return this;
    }

    /**
     * Sets the default value of the parameter.
     *
     * @param def Default value.
     * @return This object for daisy chaining.
     */
    public Parameter setDefaultValue(Value def) {
        this.defVal = def;
        return this;
    }

    /**
     * Sets the editor type of the parameter, used for parameters in an
     * action.
     *
     * @param type Editor type to set.
     * @return This object for daisy chaining.
     */
    public Parameter setEditorType(EditorType type) {
        if (type == null) {
            this.editorType = null;
            return this;
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
     * Sets the description of the parameter. Displayed to the user when
     * the parameter is hovered over.
     *
     * @param description Description of the parameter.
     * @return This object for daisy chaining.
     */
    public Parameter setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * @return The description of the parameter.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the place holder over the parameter. Displayed to the user in the
     * field box.
     *
     * @param placeHolder Place holder of the parameter.
     * @return This object for daisy chaining.
     */
    public Parameter setPlaceHolder(String placeHolder) {
        this.placeHolder = placeHolder;
        return this;
    }

    /**
     * This is used for the metadata of a column.
     *
     * @param meta Column metadata.
     * @return This object for daisy chaining.
     */
    public Parameter setMetaData(JsonObject meta) {
        this.meta = meta;
        return this;
    }

    /**
     * @return Column metadata
     */
    public JsonObject getMetaData() {
        return meta;
    }

    /**
     * @return The place holder of the parameter.
     */
    public String getPlaceHolder() {
        return placeHolder;
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
