package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.node.Node;

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
        if (name == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        char[] nameChars = name.toCharArray();
        char[] bannedChars = Node.getBannedCharacters();
        nameLoop: for (int i = 0; i < nameChars.length; ++i) {
            char nameChar = nameChars[i];
            // Skip over already encoded characters
            if (nameChar == '%' && i + 1 < nameChars.length) {
                char hexA = nameChars[i + 1];
                if ((hexA >= '0' && hexA <= '9')
                        || (hexA >= 'a' && hexA <= 'f')
                        || (hexA >= 'A' && hexA <= 'F')) {
                    if (i + 2 < nameChars.length) {
                        char hexB = nameChars[i + 2];
                        if ((hexB >= '0' && hexB <= '9')
                                || (hexB >= 'a' && hexB <= 'f')
                                || (hexB >= 'A' && hexB <= 'F')) {
                            i += 2;
                            builder.append(nameChar);
                            builder.append(hexA);
                            builder.append(hexB);
                            continue;
                        } else {
                            ++i;
                            builder.append(nameChar);
                            builder.append(hexA);
                            continue;
                        }
                    }
                }
            }
            for (char bannedChar : bannedChars) {
                if (nameChar == bannedChar) {
                    String re = Integer.toHexString(bannedChar);
                    re = re.toUpperCase();
                    builder.append('%');
                    builder.append(re);
                    continue nameLoop;
                }
            }
            builder.append(nameChar);
        }
        return builder.toString();
    }

    /**
     * Decodes a name that may contain illegal characters.
     *
     * @param name Name to decode.
     * @return Decoded name.
     */
    public static String decodeName(String name) {
        if (name == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        char[] nameChars = name.toCharArray();
        for (int i = 0; i < nameChars.length; ++i) {
            char nameChar = nameChars[i];
            if (nameChar == '%') {
                char hexA = nameChars[i + 1];
                if ((hexA >= '0' && hexA <= '9')
                        || (hexA >= 'a' && hexA <= 'f')
                        || (hexA >= 'A' && hexA <= 'F')) {
                    String s = String.valueOf(hexA);
                    if (i + 2 < nameChars.length) {
                        char hexB = nameChars[i + 2];
                        if ((hexB >= '0' && hexB <= '9')
                                || (hexB >= 'a' && hexB <= 'f')
                                || (hexB >= 'A' && hexB <= 'F')) {
                            ++i;
                            s += String.valueOf(hexB);
                        }
                    }
                    ++i;
                    int c = Integer.parseInt(s, 16);
                    builder.append((char) c);
                    continue;
                }
            }

            builder.append(nameChar);
        }

        return builder.toString();
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

        if (strings.length <= 0) {
            return "";
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
