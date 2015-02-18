package org.dsa.iot.dslink.node.value;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ValueStatus {

    OK("ok"),
    STALE("stale"),
    DISCONNECTED("disconnected");

    @Getter
    private final String jsonName;
}
