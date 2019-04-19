package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the URL information parser.
 *
 * @author Samuel Grenier
 */
public class URLInfoTest {

    @Test
    public void certificateTrust() {
        URLInfo info = URLInfo.parse("http://localhost:8080");
        Assert.assertTrue(info.getTrustAllCertificates());
        info.setTrustAllCertificates(false);
        Assert.assertFalse(info.getTrustAllCertificates());
    }

    @Test(expected = RuntimeException.class)
    public void invalidUrl() {
        URLInfo.parse("localhost:8080");
    }

    @Test
    public void nullScheme() {
        try {
            URLInfo.getDefaultPort(null);
        } catch (NullPointerException e) {
            Assert.assertEquals("scheme", e.getMessage());
        }

        try {
            URLInfo.getDefaultProtocolSecurity(null);
        } catch (NullPointerException e) {
            Assert.assertEquals("scheme", e.getMessage());
        }
    }

    /**
     * Tests the default ports
     */
    @Test
    public void portTest() {
        int port = URLInfo.getDefaultPort("ws");
        Assert.assertEquals(80, port);

        port = URLInfo.getDefaultPort("wss");
        Assert.assertEquals(443, port);

        port = URLInfo.getDefaultPort("http");
        Assert.assertEquals(80, port);

        port = URLInfo.getDefaultPort("https");
        Assert.assertEquals(443, port);

        port = URLInfo.getDefaultPort("unknown");
        Assert.assertEquals(-1, port);
    }

    @Test
    public void secureScheme() {
        Assert.assertTrue(URLInfo.getDefaultProtocolSecurity("wss"));
        Assert.assertTrue(URLInfo.getDefaultProtocolSecurity("https"));

        Assert.assertFalse(URLInfo.getDefaultProtocolSecurity("http"));
        Assert.assertFalse(URLInfo.getDefaultProtocolSecurity("ws"));
    }

    /**
     * Generic url parsing tests to ensure parsed data is always consistent.
     */
    @Test
    public void urlParser() {
        URLInfo[] info = new URLInfo[]{
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

    /**
     * Tests to ensure that the SSL override over the URL can override
     * any protocol scheme such as https.
     */
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
