package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Samuel Grenier
 */
public class StringUtilsTest {

    @Test
    public void filter() {
        String s = StringUtils.filterBannedChars(".test");
        Assert.assertEquals("test", s);
    }

    @Test
    public void encode() {
        String s = "% . / \\ ? * : | < > $ @";
        s = StringUtils.encodeName(s);
        Assert.assertEquals("%25 %2E %2F %5C %3F %2A %3A %7C %3C %3E %24 %40", s);
    }

    @Test
    public void decode() {
        String s = "%25 %2E %2F %5C %3F %2A %3A %7C %3C %3E %24 %40 %25";
        s = StringUtils.decodeName(s);
        Assert.assertEquals("% . / \\ ? * : | < > $ @ %", s);
    }

    @Test
    public void references() {
        Assert.assertTrue(StringUtils.isReference("$test"));
        Assert.assertTrue(StringUtils.isReference("@test"));
        Assert.assertFalse(StringUtils.isReference("test"));

        try {
            StringUtils.isReference(null);
        } catch (NullPointerException e) {
            Assert.assertEquals("name", e.getMessage());
        }
    }

    @Test
    public void join() {
        try {
            StringUtils.join((Set<String>) null, null);
        } catch (NullPointerException e) {
            Assert.assertEquals("strings", e.getMessage());
        }

        try {
            StringUtils.join((String[]) null, null);
        } catch (NullPointerException e) {
            Assert.assertEquals("strings", e.getMessage());
        }

        try {
            StringUtils.join(new HashSet<String>(), null);
        } catch (NullPointerException e) {
            Assert.assertEquals("delimiter", e.getMessage());
        }

        {
            String s = StringUtils.join(new String[0], "|");
            Assert.assertEquals("", s);
        }

        {
            String s = StringUtils.join(new LinkedHashSet<String>(), "|");
            Assert.assertEquals("", s);
        }

        {
            Set<String> strings = new LinkedHashSet<>();
            strings.add("1");
            strings.add("2");
            strings.add("3");
            Assert.assertEquals("1|2|3", StringUtils.join(strings, "|"));
        }
    }

    @Test
    public void contains() {
        try {
            StringUtils.contains(null, null);
        } catch (NullPointerException e) {
            Assert.assertEquals("chars", e.getMessage());
        }

        final char[] test = new char[] {
                'a',
                'b',
                'c',
        };

        Assert.assertFalse(StringUtils.contains("", test));
        Assert.assertFalse(StringUtils.contains("d", test));
        Assert.assertTrue(StringUtils.contains("a", test));
        Assert.assertTrue(StringUtils.contains("b", test));
        Assert.assertTrue(StringUtils.contains("c", test));
        Assert.assertTrue(StringUtils.contains("abc", test));
    }
}
