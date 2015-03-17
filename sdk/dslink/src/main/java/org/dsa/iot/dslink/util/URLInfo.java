package org.dsa.iot.dslink.util;

/**
 * URL information parsed from a string.
 * @author Samuel Grenier
 */
public class URLInfo {

    public final String protocol;
    public final String host;
    public final int port;
    public final String path;
    public final boolean secure;
    private boolean trustAllCertificates = true;

    /**
     * Populates the URL information object
     * @param protocol Protocol of the URL
     * @param host Host of the URL
     * @param port Port of the URL (or default if none provided)
     * @param path Path of the URL
     * @param secure Whether the URL connects over a secure channel or not
     */
    private URLInfo(String protocol, String host,
                    int port, String path, boolean secure) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.secure = secure;
    }

    /**
     * This is effective only if {@link #secure} is {@code true}. When
     * an outgoing connection is being made, it must obey this property.
     * @param trust Whether to trust all certificates or not.
     */
    public void setTrustAllCertificates(boolean trust) {
        this.trustAllCertificates = trust;
    }

    /**
     * This is effective only if {@link #secure} is {@code true}. When an
     * outgoing connection is being made, it must obey this property.
     * @return Whether to trust all certificates or not.
     */
    public boolean getTrustAllCertificates() {
        return trustAllCertificates;
    }

    /**
     * Parses the URL into an object with its separated components.
     * @param url URL to parse
     * @return The parsed URL ready for data consumption
     */
    public static URLInfo parse(String url) {
        return parse(url, null);
    }

    /**
     * @param url URL to parse
     * @param secureOverride Set to null to use default security detection
     * @return An information object about a URL.
     */
    public static URLInfo parse(String url, Boolean secureOverride) {
        String[] parts = url.split("://");
        if (!url.contains("://") || parts.length > 2)
            throw new RuntimeException("Invalid URL");

        String protocol = parts[0];
        boolean secure;
        if (secureOverride != null)
            secure = secureOverride;
        else
            secure = getDefaultProtocolSecurity(protocol);

        String host = parts[1];
        String path = "/";
        int port = -1;

        if (host.contains(":")) {
            int index = host.indexOf(':');
            host = host.substring(0, index);

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
            port = getDefaultPort(protocol);
        return new URLInfo(protocol, host, port, path, secure);
    }

    /**
     *
     * @param scheme Scheme to test
     * @return Whether the scheme/protocol is supposed to be over SSL or not by
     *         default.
     */
    public static boolean getDefaultProtocolSecurity(String scheme) {
        if (scheme == null)
            throw new NullPointerException("scheme");
        return "ws".equals(scheme) || "https".equals(scheme);
    }

    /**
     * Gets the default port of a specified protocol URI scheme.
     * @param scheme Scheme to get the default port of.
     * @return Default port of the scheme, or -1 if unsupported.
     */
    public static int getDefaultPort(String scheme) {
        if (scheme == null)
            throw new NullPointerException("scheme");
        if ("ws".equals(scheme) || "http".equals(scheme))
            return 80;
        else if ("wss".equals(scheme) || "https".equals(scheme))
            return 443;
        return -1;
    }
}