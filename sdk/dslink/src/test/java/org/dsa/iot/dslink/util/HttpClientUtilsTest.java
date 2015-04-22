package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.http.HttpClient;

/**
 * @author Samuel Grenier
 */
public class HttpClientUtilsTest {

    @Before
    public void setup() {
        // Coverage
        new HttpClientUtils();
    }

    @Test
    public void configure() {
        URLInfo info = URLInfo.parse("https://localhost:8080/conn");
        info.setTrustAllCertificates(true);

        HttpClient client = HttpClientUtils.configure(info);
        Assert.assertTrue(client.isSSL());
        Assert.assertTrue(client.isTrustAll());
        Assert.assertFalse(client.isVerifyHost());

        Assert.assertEquals(8080, client.getPort());
        Assert.assertEquals(Integer.MAX_VALUE, client.getMaxWebSocketFrameSize());
        Assert.assertEquals(info.host, client.getHost());
    }

}
