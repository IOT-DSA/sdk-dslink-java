package org.dsa.iot.core;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class URLTest {

    @Before
    public void init() {
        Logger.getRootLogger().setLevel(Level.OFF);
    }

    @Test
    public void urlParser() {
        URLInfo[] info = new URLInfo[] {
            URLInfo.parse("https://localhost"),
            URLInfo.parse("https://localhost/"),
            URLInfo.parse("https://localhost:443"),
            URLInfo.parse("https://localhost:443/")
        };

        for (URLInfo i : info) {
            Assert.assertEquals("localhost", i.host);
            Assert.assertEquals("/", i.path);
            Assert.assertEquals("https", i.protocol);
            Assert.assertEquals(443, i.port);
            Assert.assertTrue(i.secure);
        }
    }

    @Test
    public void urlParserOverride() {
        URLInfo info = URLInfo.parse("https://localhost:8080/test", false);

        Assert.assertEquals(info.host, "localhost");
        Assert.assertEquals(info.path, "/test");
        Assert.assertEquals(info.protocol, "https");
        Assert.assertEquals(info.port, 8080);
        Assert.assertFalse(info.secure);
    }
}
