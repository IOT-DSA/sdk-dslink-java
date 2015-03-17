package org.dsa.iot.dslink.util;

/**
 * String utilities for manipulating strings
 * @author Samuel Grenier
 */
public class StringUtils {

    /**
     * @param string String to check
     * @param chars Characters to look for
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
