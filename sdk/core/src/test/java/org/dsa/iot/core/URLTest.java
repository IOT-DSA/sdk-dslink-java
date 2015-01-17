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
        URLInfo info = URLInfo.parse("https://localhost");

        Assert.assertEquals(info.host, "localhost");
        Assert.assertEquals(info.path, "/");
        Assert.assertEquals(info.protocol, "https");
        Assert.assertEquals(info.port, 443);
        Assert.assertTrue(info.secure);
    }

    @Test
    public void urlParserOverride() {
        URLInfo info = URLInfo.parse("https://localhost:80/test", false);

        Assert.assertEquals(info.host, "localhost");
        Assert.assertEquals(info.path, "/test");
        Assert.assertEquals(info.protocol, "https");
        Assert.assertEquals(info.port, 80);
        Assert.assertFalse(info.secure);
    }
}
