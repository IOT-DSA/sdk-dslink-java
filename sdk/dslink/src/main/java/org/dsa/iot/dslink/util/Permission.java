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

    public static Permission toEnum(String perm) {
        switch (perm) {
            case "none":
                return NONE;
            case "read":
                return READ;
            case "write":
                return WRITE;
            case "config":
                return CONFIG;
            case "never":
                return NEVER;
            default:
                throw new RuntimeException("Unhandled type");
        }
    }
}
