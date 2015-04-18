package org.dsa.iot.dslink.util;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;

/**
 * Client utilities for {@link org.vertx.java.core.http.HttpClient}
 *
 * @author Samuel Grenier
 */
public class HttpClientUtils {

    /**
     * Configures a client according to the designated URL.
     *
     * @param url URL to configure an {@link HttpClient} to.
     * @return A configured HTTP client ready to make an outgoing connection
     *         to its destination.
     */
    public static HttpClient configure(URLInfo url) {
        Vertx vertx = Objects.getVertx();
        HttpClient client = vertx.createHttpClient();
        client.setMaxWebSocketFrameSize(Integer.MAX_VALUE);
        client.setPort(url.port);
        client.setHost(url.host);
        if (url.secure) {
            client.setSSL(true);
            client.setVerifyHost(false);
            client.setTrustAll(url.getTrustAllCertificates());
        }
        return client;
    }

}
