package org.dsa.iot.dslink.methods;

/**
 * Possible states that a method can be in.
 *
 * @author Samuel Grenier
 */
public enum StreamState {

    INITIALIZED("initialize"),
    OPEN("open"),
    CLOSED("closed");

    private final String jsonName;

    StreamState(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }
}
