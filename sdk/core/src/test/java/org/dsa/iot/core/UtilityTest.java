package org.dsa.iot.core;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class UtilityTest {

    @Test
    public void portDefaults() {
        int https = Utils.getDefaultPort("https");
        int http = Utils.getDefaultPort("http");
        int ws = Utils.getDefaultPort("ws");
        int wss = Utils.getDefaultPort("wss");

        Assert.assertEquals(443, https);
        Assert.assertEquals(80, http);

        Assert.assertEquals(443, wss);
        Assert.assertEquals(80, ws);
    }

}
