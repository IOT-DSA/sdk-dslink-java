package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Samuel Grenier
 */
public class StringUtilsTest {

    @Test
    public void encode() {
        String s = "% . / \\ ? * : | < > $ @";
        s = StringUtils.encodeName(s);
        assertEquals("%25 %2E %2F %5C %3F %2A %3A %7C %3C %3E %24 %40", s);
    }

    @Test
    public void decode() {
        String s = "% %25 %2E %2F %5C %3F %2A %3A %7C %3C %3E %24 %40 %25";
        s = StringUtils.decodeName(s);
        assertEquals("% % . / \\ ? * : | < > $ @ %", s);
    }

    @Test
    public void references() {
        Assert.assertTrue(StringUtils.isReference("$test"));
        Assert.assertTrue(StringUtils.isReference("@test"));
        Assert.assertFalse(StringUtils.isReference("test"));

        try {
            StringUtils.isReference(null);
        } catch (NullPointerException e) {
            assertEquals("name", e.getMessage());
        }
    }

    @Test
    public void new_encodeName_withDot() {
        String toEncode = "/data/a%2Eb";
        String expected = "%2Fdata%2Fa%252Eb";

        String actual = StringUtils.encodeName(toEncode);

        assertEquals(expected, actual);
    }

    @Test
    public void new_decodeName_withDot() {
        String toDecode = "%2Fdata%2Fa%252Eb";
        String expected = "/data/a%2Eb";

        String actual = StringUtils.decodeName(toDecode);

        assertEquals(expected, actual);
    }

    @Test
    public void join() {
        try {
            StringUtils.join((Set<String>) null, null);
        } catch (NullPointerException e) {
            assertEquals("strings", e.getMessage());
        }

        try {
            StringUtils.join((String[]) null, null);
        } catch (NullPointerException e) {
            assertEquals("strings", e.getMessage());
        }

        try {
            StringUtils.join(new HashSet<String>(), null);
        } catch (NullPointerException e) {
            assertEquals("delimiter", e.getMessage());
        }

        {
            String s = StringUtils.join(new String[0], "|");
            assertEquals("", s);
        }

        {
            String s = StringUtils.join(new LinkedHashSet<String>(), "|");
            assertEquals("", s);
        }

        {
            Set<String> strings = new LinkedHashSet<>();
            strings.add("1");
            strings.add("2");
            strings.add("3");
            assertEquals("1|2|3", StringUtils.join(strings, "|"));
        }
    }

    @Test
    public void camelCaseToDisplay() {
        assertEquals("Hello World", StringUtils.camelCaseToDisplay("helloWorld"));
        assertEquals("Hello World", StringUtils.camelCaseToDisplay("HelloWorld"));
        assertEquals("Hello WWorld", StringUtils.camelCaseToDisplay("HelloWWorld"));
    }
}
