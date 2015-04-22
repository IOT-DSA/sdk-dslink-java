package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Samuel Grenier
 */
public class StringUtilsTest {

    @Before
    public void setup() {
        // Coverage
        new StringUtils();
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
            StringUtils.join(null, null);
        } catch (NullPointerException e) {
            Assert.assertEquals("strings", e.getMessage());
        }

        try {
            StringUtils.join(new HashSet<String>(), null);
        } catch (NullPointerException e) {
            Assert.assertEquals("delimiter", e.getMessage());
        }

        Set<String> strings = new HashSet<>();
        strings.add("1");
        strings.add("2");
        strings.add("3");
        Assert.assertEquals("1|2|3", StringUtils.join(strings, "|"));
    }

    @Test
    public void contains() {
        try {
            StringUtils.contains(null, null);
        } catch (NullPointerException e) {
            Assert.assertEquals("chars", e.getMessage());
        }

        final String[] test = new String[] {
                "a",
                "b",
                "c",
                "abc"
        };

        Assert.assertFalse(StringUtils.contains("", test));
        Assert.assertFalse(StringUtils.contains("d", test));
        Assert.assertTrue(StringUtils.contains("a", test));
        Assert.assertTrue(StringUtils.contains("b", test));
        Assert.assertTrue(StringUtils.contains("c", test));
        Assert.assertTrue(StringUtils.contains("abc", test));
    }
}
