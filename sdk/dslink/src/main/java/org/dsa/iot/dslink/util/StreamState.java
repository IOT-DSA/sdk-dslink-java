package org.dsa.iot.dslink.util;

/**
 * @author Samuel Grenier
 */
public enum StreamState {
    INITIALIZED("initialize"),
    OPEN("open"),
    CLOSED("closed");

    private final String jsonName;

    private StreamState(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }
}
