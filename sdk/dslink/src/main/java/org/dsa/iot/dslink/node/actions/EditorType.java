package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class EditorType {

    public static final EditorType TEXT_AREA = new EditorType("textarea");
    public static final EditorType PASSWORD = new EditorType("password");
    public static final EditorType DATE_RANGE = new EditorType("daterange");
    public static final EditorType DATE = new EditorType("date");
    public static final EditorType FILE_INPUT = new EditorType("fileinput");

    private final String type;
    private final JsonObject meta;

    private EditorType(String type, JsonObject meta) {
        this.type = type;
        this.meta = meta;
    }

    private EditorType(String type) {
        this(type, null);
    }

    public String toJsonString() {
        return type;
    }

    /**
     * In case an editor type is not supported, it can easily be created using
     * this factory method.
     *
     * @param type Type of editor.
     * @return An editor type.
     */
    public static EditorType make(String type) {
        return new EditorType(type);
    }

    /**
     * In case an editor type is not supported, it can easily be created using
     * this factory method.
     *
     * @param type Type of editor.
     * @param meta Metadata for the editor.
     * @return An editor type.
     */
    public static EditorType make(String type, JsonObject meta) {
        return new EditorType(type, meta);
    }

    public JsonObject getMeta() {
        return meta;
    }
}
