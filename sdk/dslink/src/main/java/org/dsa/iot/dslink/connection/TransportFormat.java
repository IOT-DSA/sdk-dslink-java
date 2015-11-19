package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.util.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public enum TransportFormat {

    MESSAGE_PACK("msgpack"),
    JSON("json");

    private final String name;

    TransportFormat(String name) {
        this.name = name;
    }

    public String toJson() {
        return name;
    }

    public static JsonArray toJsonArray() {
        JsonArray array = new JsonArray();
        for (TransportFormat format : TransportFormat.values()) {
            array.add(format.toJson());
        }
        return array;
    }

    public static TransportFormat toEnum(String format) {
        if (format == null) {
            return JSON;
        }
        format = format.toLowerCase();
        if (MESSAGE_PACK.name.equals(format)) {
            return MESSAGE_PACK;
        } else if (JSON.name.equals(format)) {
            return JSON;
        }
        return JSON;
    }
}
