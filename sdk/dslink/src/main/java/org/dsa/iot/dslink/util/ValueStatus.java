package org.dsa.iot.dslink.util;

/**
 * @author Samuel Grenier
 */
public enum ValueStatus {

    OK("ok"),
    STALE("stale"),
    DISCONNECTED("disconnected");

    public final String jsonName;

    private ValueStatus(String jsonName) {
        this.jsonName = jsonName;
    }
}
