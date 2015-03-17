package org.dsa.iot.dslink.node.value;

/**
 * @author Samuel Grenier
 */
public enum ValueStatus {

    OK("ok"),
    STALE("stale"),
    DISCONNECTED("disconnected");

    private final String jsonName;

    ValueStatus(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }
}
