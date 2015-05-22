package org.dsa.iot.dslink.node.actions;

/**
 * @author Samuel Grenier
 */
public class EditorType {

    public static final EditorType TEXT_AREA = new EditorType("textarea");
    public static final EditorType PASSWORD = new EditorType("password");
    public static final EditorType DATE_RANGE = new EditorType("daterange");
    public static final EditorType DATE = new EditorType("date");

    private final String type;

    private EditorType(String type) {
        this.type = type;
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
}
