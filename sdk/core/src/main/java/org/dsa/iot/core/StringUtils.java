package org.dsa.iot.core;

import lombok.NonNull;

/**
 * @author Samuel Grenier
 */
public class StringUtils {

    private static final String[] BANNED_CHARS = new String[] {
        ".", "/", "\\", "?", "%", "*", ":", "|", "“", "<", ">"
    };

    public static void checkEmpty(@NonNull String name) {
        if (name.isEmpty())
            throw new IllegalArgumentException("empty name");
    }

    public static void checkNodeName(@NonNull String name) {
        checkEmpty(name);
        if (contains(name, BANNED_CHARS)
                || name.startsWith("@") || name.startsWith("$"))
            throw new IllegalArgumentException("invalid name");
    }

    public static boolean isAttribOrConf(@NonNull String name) {
        return name.startsWith("$") || name.startsWith("@");
    }

    public static boolean contains(String string, String[] chars) {
        for (String s : chars) {
            if (string.contains(s))
                return true;
        }
        return false;
    }
}
