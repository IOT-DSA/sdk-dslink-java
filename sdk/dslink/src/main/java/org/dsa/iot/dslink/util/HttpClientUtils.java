package org.dsa.iot.dslink.util;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpClient;

/**
 * Client utilities for {@link org.vertx.java.core.http.HttpClient}
 * @author Samuel Grenier
 */
public class HttpClientUtils {

    /**
     * Configures a client according to the designated URL.
     * @param url URL to configure an {@link HttpClient} to
     */
    public static HttpClient configure(URLInfo url) {
        Vertx vertx = VertxFactory.newVertx();
        HttpClient client = vertx.createHttpClient();
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
