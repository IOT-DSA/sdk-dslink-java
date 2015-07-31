package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.node.Node;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;
import java.util.regex.Pattern;

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
     * Encodes a name so it will not have illegal characters.
     *
     * @param name Name to encode.
     * @return Encoded name.
     */
    public static String encodeName(String name) {
        for (char c : Node.getBannedCharacters()) {
            String banned = String.valueOf(c);
            if (name.contains(banned)) {
                String replacement = "%";
                replacement += Integer.toHexString(c).toUpperCase();
                banned = Pattern.quote(banned);
                name = name.replaceAll(banned, replacement);
            }
        }
        return name;
    }

    /**
     * Decodes a name that may contain illegal characters.
     *
     * @param name Name to decode.
     * @return Decoded name.
     */
    public static String decodeName(String name) {
        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Filters names based on banned characters for node names.
     *
     * @param name Name to filter.
     * @return Filtered text.
     * @see Node#getBannedCharacters
     */
    public static String filterBannedChars(String name) {
        return filterBannedChars(name, Node.getBannedCharacters(), "");
    }

    /**
     *
     * @param name Name to filter.
     * @param chars Banned strings that are not allowed to be in the name.
     * @param replacement What to replace the bad text with.
     * @return Filtered text.
     */
    public static String filterBannedChars(String name,
                                           char[] chars,
                                           String replacement) {
        for (char banned : chars) {
            String s = String.valueOf(banned);
            if (name.contains(String.valueOf(banned))) {
                name = name.replaceAll(Pattern.quote(s), replacement);
            }
        }
        return name;
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

        int size = strings.size();
        String[] array = strings.toArray(new String[size]);
        return join(array, delimiter);
    }

    /**
     * Joins strings together into a single string using a designated
     * builder.
     *
     * @param strings Strings to join
     * @param delimiter Delimiter to join them by
     * @return A single built string
     */
    public static String join(String[] strings, String delimiter) {
        if (strings == null) {
            throw new NullPointerException("strings");
        } else if (delimiter == null) {
            throw new NullPointerException("delimiter");
        }

        StringBuilder builder = new StringBuilder();
        int size = strings.length;
        for (int i = 0;;) {
            builder.append(strings[i]);
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
    public static boolean contains(String string, char[] chars) {
        if (chars == null) {
            throw new NullPointerException("chars");
        } else if (string == null || string.isEmpty() || chars.length == 0) {
            return false;
        } else {
            for (char c : chars) {
                String s = String.valueOf(c);
                if (string.contains(s)) {
                    return true;
                }
            }
            return false;
        }
    }

}
