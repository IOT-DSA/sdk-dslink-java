package org.dsa.iot.dslink.provider;

import org.dsa.iot.dslink.provider.netty.DefaultHttpProvider;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.http.HttpResp;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public abstract class HttpProvider {

    private static HttpProvider PROVIDER;

    public static HttpProvider getProvider() {
        if (PROVIDER == null) {
            setProvider(new DefaultHttpProvider());
        }
        return PROVIDER;
    }

    public static void setProvider(HttpProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        PROVIDER = provider;
    }

    /**
     * Posts data to the designated address and immediately closes
     * the connection.
     *
     * @param url URL to post to.
     * @param content Content of the body or {@code null}.
     * @param headers Headers to send or {@code null}.
     * @return A response from the HTTP server.
     */
    public abstract HttpResp post(URLInfo url,
                                  byte[] content,
                                  Map<String, String> headers);
}
