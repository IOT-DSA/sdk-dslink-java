package org.dsa.iot.core;

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
        if (!url.contains("://"))
            throw new RuntimeException("Invalid URL");
        String[] parts = url.split("://");
        if (parts.length > 2)
            throw new RuntimeException("Invalid URL");

        String protocol = parts[0];
        boolean secure = false;
        if (secureOverride != null)
            secure = secureOverride;
        else if ("wss".equals(protocol) || "https".equals(protocol))
            secure = true;

        String host = parts[1];
        String path = "/";
        int port = -1;

        if (parts[1].contains(":")) {
            int index = parts[1].indexOf(':');
            host = parts[1].substring(0, index);

            // Secondary can contain port and path
            String secondary = parts[1].substring(index + 1);
            if (secondary.contains("/")) {
                index = secondary.indexOf('/');
                port = Integer.parseInt(secondary.substring(0, index));
                path = secondary.substring(index);
            } else {
                port = Integer.parseInt(secondary);
            }
        } else if (host.contains("/")) {
            int index = host.indexOf('/');
            host = host.substring(0, index);
            path = host.substring(index);
            if (path.isEmpty())
                path = "/";
        }

        if (port == -1)
            port = Utils.getDefaultPort(protocol);
        return new URLInfo(protocol, host, port, path, secure);
    }
}
