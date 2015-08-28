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

    public static StreamState toEnum(String stream) {
        if (stream == null) {
            return null;
        }
        switch (stream) {
            case "initialize":
                return INITIALIZED;
            case "open":
                return OPEN;
            case "closed":
                return CLOSED;
            default:
                throw new RuntimeException("Unknown stream type: " + stream);
        }
    }
}
