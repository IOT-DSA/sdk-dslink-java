package org.dsa.iot.dslink.util.json;

/**
 * @author Samuel Grenier
 */
public enum EncodingFormat {

    JSON("json");

    private final String name;

    EncodingFormat(String name) {
        this.name = name;
    }

    public String toJson() {
        return name;
    }

    public static JsonArray toJsonArray() {
        JsonArray array = new JsonArray();
        for (EncodingFormat format : EncodingFormat.values()) {
            array.add(format.toJson());
        }
        return array;
    }

    public static EncodingFormat toEnum(String format) {
        if (format == null) {
            return JSON;
        }
        format = format.toLowerCase();
        if (JSON.name.equals(format)) {
            return JSON;
        }
        return JSON;
    }
}
