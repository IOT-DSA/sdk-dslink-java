package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.node.Node;

import java.util.Collection;

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
     * @param string String to encode.
     * @return Encoded name.
     */
    public static String encodeName(String string) {
        return encodeName(string, Node.getBannedCharacters());
    }

    /**
     * Encodes a name so it will not have illegal characters.
     *
     * @param string String to encode.
     * @param encode Characters to encode in the name.
     * @return Encoded name.
     */
    public static String encodeName(String string, char[] encode) {
        String tmp = string.replaceAll("%", "%25");
        return encodeNameOld(tmp, Node.getBannedCharacters());
    }

    public static String encodeNameOld(String string, char[] encode) {
        if (string == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        char[] nameChars = string.toCharArray();
        nameLoop:
        for (int i = 0; i < nameChars.length; ++i) {
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
            for (char c : encode) {
                if (nameChar == c) {
                    String re = Integer.toHexString(c);
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
     * @param string String to decode.
     * @return Decoded name.
     */
    public static String decodeName(String string) {
        if (string == null) {
            return null;
        }

        String tmp = decodeNameOld(string);
        return tmp.replaceAll("%25", "%");
    }

    public static String decodeNameOld(String string) {
        if (string == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        char[] nameChars = string.toCharArray();
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
     * Joins strings together into a single string using a designated
     * delimiter.
     *
     * @param strings Strings to join.
     * @param delimiter Delimiter to join each element by.
     * @return A single built string.
     */
    public static String join(Collection<String> strings, String delimiter) {
        return join(strings, false, delimiter);
    }

    /**
     * Joins strings together into a single string using a designated
     * delimiter.
     *
     * @param strings Strings to join.
     * @param encode Whether or not to encode each element.
     * @param delimiter Delimiter to join them by.
     * @return A single built string.
     */
    public static String join(Collection<String> strings,
                              boolean encode, String delimiter) {
        if (strings == null) {
            throw new NullPointerException("strings");
        } else if (delimiter == null) {
            throw new NullPointerException("delimiter");
        }

        int size = strings.size();
        String[] array = strings.toArray(new String[size]);
        return join(array, encode, delimiter);
    }

    /**
     * Joins strings together into a single string using a designated
     * delimiter.
     *
     * @param strings Strings to join.
     * @param delimiter  Delimiter to join each element by.
     * @return A single built string.
     */
    public static String join(String[] strings, String delimiter) {
        return join(strings, false, delimiter);
    }

    /**
     * Joins strings together into a single string using a designated
     * delimiter.
     *
     * @param strings Strings to join.
     * @param encode Whether or not to encode each element.
     * @param delimiter Delimiter to join them by.
     * @return A single built string.
     */
    public static String join(String[] strings,
                              boolean encode,
                              String delimiter) {
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
            String s = strings[i];
            if (encode) {
                s = encodeName(s);
            }
            builder.append(s);
            if (++i < size) {
                builder.append(delimiter);
            } else {
                break;
            }
        }

        return builder.toString();
    }
    
    public static String[] split(String string, boolean decode, String delimiter) {
    	if (string == null) {
            throw new NullPointerException("string");
        } else if (delimiter == null) {
            throw new NullPointerException("delimiter");
        }
    	
    	String[] arr = string.split(delimiter);
    	if (decode) {
    		for (int i = 0; i < arr.length; i++) {
    			arr[i] = decodeName(arr[i]);
    		}
    	}
    	return arr;
    }

    public static String camelCaseToDisplay(String camelCase) {
        StringBuilder buf = new StringBuilder();
        char ch;
        boolean lastUpper = false;
        for (int i = 0, len = camelCase.length(); i < len; i++) {
            ch = camelCase.charAt(i);
            if (i == 0) {
                ch = Character.toUpperCase(ch);
                lastUpper = true;
            } else if (Character.isUpperCase(ch)) {
                if (!lastUpper) {
                    lastUpper = true;
                    buf.append(' ');
                }
            } else {
                lastUpper = false;
            }
            buf.append(ch);
        }
        return buf.toString();
    }

}
