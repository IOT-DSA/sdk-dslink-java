package org.dsa.iot.core;

import lombok.NonNull;
import lombok.val;

/**
 * @author Samuel Grenier
 */
public class StringUtils {

    private static final String[] BANNED_CHARS = new String[] { ".", "/", "\\",
            "?", "%", "*", ":", "|", "<", ">" };

    public static String join(int start, String[] parts) {
        return join(start, parts.length, parts);
    }

    public static String join(int start, String delimiter, String[] parts) {
        return join(start, parts.length, delimiter, parts);
    }

    public static String join(int start, int end, String[] parts) {
        return join(start, end, null, parts);
    }

    public static String join(int start, int end, String delimiter,
            @NonNull String[] parts) {
        val builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            builder.append(parts[i]);
            if (delimiter != null && i < (end - 1)) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }

    public static void checkEmpty(@NonNull String name) {
        if (name.isEmpty())
            throw new IllegalArgumentException("empty name");
    }

    public static void checkNodeName(@NonNull String name) {
        checkEmpty(name);
        if (contains(name, BANNED_CHARS) || name.startsWith("@")
                || name.startsWith("$"))
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

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
