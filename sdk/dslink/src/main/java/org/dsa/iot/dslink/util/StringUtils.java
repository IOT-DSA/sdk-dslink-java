package org.dsa.iot.dslink.util;

import java.util.Set;

/**
 * String utilities for manipulating strings
 *
 * @author Samuel Grenier
 */
public class StringUtils {

    /**
     * Tests the designated name for whether it is a reference or not.
     *
     * @param name Name to test.
     * @return Whether the name is a reference to a configuration or attribute.
     */
    public static boolean isReference(String name) {
        if (name == null)
            throw new NullPointerException("name");
        return name.startsWith("$") || name.startsWith("@");
    }

    /**
     * Joins strings together into a single string using a designated
     * builder.
     *
     * @param strings Strings to join
     * @param delimiter Delimiter to join them by
     * @return A single built string
     */
    public static String join(Set<String> strings, String delimiter) {
        if (strings == null) {
            throw new NullPointerException("strings");
        } else if (delimiter == null) {
            throw new NullPointerException("delimiter");
        }

        StringBuilder builder = new StringBuilder();
        int size = strings.size();
        String[] array = strings.toArray(new String[size]);
        for (int i = 0;;) {
            builder.append(array[i]);
            if (++i < size) {
                builder.append(delimiter);
            } else {
                break;
            }
        }

        return builder.toString();
    }

    /**
     * @param string String to check
     * @param chars  Characters or strings to look for
     * @return Whether the string contains any of the designated characters.
     */
    public static boolean contains(String string, String[] chars) {
        if (chars == null) {
            throw new NullPointerException("chars");
        } else if (string == null || string.isEmpty() || chars.length == 0) {
            return false;
        } else {
            for (String s : chars) {
                if (string.contains(s)) {
                    return true;
                }
            }
            return false;
        }
    }

}
