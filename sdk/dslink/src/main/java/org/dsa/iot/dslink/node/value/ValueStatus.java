package org.dsa.iot.dslink.node.value;

/**
 * Status of the value
 * @author Samuel Grenier
 */
public enum ValueStatus {

    /**
     * No issues with the value.
     */
    OK("ok"),

    /**
     * The value could be potentially out of date. This indicates that a link
     * failed to update its value.
     */
    STALE("stale"),

    /**
     * Used in the broker. Designates a value as disconnected and therefore it
     * cannot be updated from the appropriate client.
     */
    DISCONNECTED("disconnected");

    private final String jsonName;

    ValueStatus(String jsonName) {
        this.jsonName = jsonName;
    }

    /**
     * @return JSON name used in the DSA protocol
     */
    public String getJsonName() {
        return jsonName;
    }
}
