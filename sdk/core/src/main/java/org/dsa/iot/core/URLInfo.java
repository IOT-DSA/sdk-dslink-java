package org.dsa.iot.core;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Samuel Grenier
 */
public class URLInfo {

    public final String protocol;
    public final String host;
    public final int port;
    public final String path;
    public final boolean secure;

    private URLInfo(String protocol, String host,
                   int port, String path, boolean secure) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.secure = secure;
    }

    public static URLInfo parse(String url) {
        return parse(url, null);
    }

    /**
     * @param url URL to parse
     * @param secureOverride Set to null to use default security detection
     * @return An information object about a URL.
     */
    public static URLInfo parse(String url, Boolean secureOverride) {
        URL u = getURL(url);

        String protocol = u.getProtocol();
        boolean secure = false;
        if (secureOverride != null)
            secure = secureOverride;
        else if ("wss".equals(protocol) || "https".equals(protocol))
            secure = true;

        String host = u.getHost();
        int port = u.getPort();
        if (port == -1)
            port = Utils.getDefaultPort(u.getProtocol());

        String path = u.getPath();
        if (path == null || path.isEmpty())
            path = "/";
        return new URLInfo(protocol, host, port, path, secure);
    }

    public static URL getURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
