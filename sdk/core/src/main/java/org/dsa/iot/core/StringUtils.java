package org.dsa.iot.core;

import lombok.NonNull;

/**
 * @author Samuel Grenier
 */
public class StringUtils {

    public static void checkEmpty(@NonNull String name) {
        if (name.isEmpty())
            throw new IllegalArgumentException("empty name");
    }

    public static void checkNodeName(@NonNull String name) {
        checkEmpty(name);
        if (name.contains("/") || name.startsWith("@") || name.startsWith("$"))
            throw new IllegalArgumentException("invalid name");
    }

    public static boolean isAttribOrConf(@NonNull String name) {
        return name.startsWith("$") || name.startsWith("@");
    }
}
