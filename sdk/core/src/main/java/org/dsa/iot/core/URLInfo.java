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

    public URLInfo(String protocol, String host, int port, String path) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
    }

    public static URLInfo parse(String url) {
        URL u = getURL(url);

        String protocol = u.getProtocol();
        String host = u.getHost();
        int port = u.getPort();
        if (port == -1)
            port = Utils.getDefaultPort(u.getProtocol());

        String path = u.getPath();
        if (path == null || path.isEmpty())
            path = "/";
        return new URLInfo(protocol, host, port, path);
    }

    public static URL getURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
