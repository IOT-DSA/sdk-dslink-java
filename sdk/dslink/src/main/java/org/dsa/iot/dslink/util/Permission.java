package org.dsa.iot.dslink.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Permission {
    NONE("none"),
    READ("read"),
    WRITE("write"),
    CONFIG("config"),
    NEVER("never");

    private final String jsonName;
}
