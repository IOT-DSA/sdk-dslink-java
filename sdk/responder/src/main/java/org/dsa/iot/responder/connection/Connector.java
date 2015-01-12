package org.dsa.iot.responder.connection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A common connection class to handle multiple transport protocols.
 * @author Samuel Grenier
 */
public abstract class Connector {

    public final String url;
    public final boolean secure;

    /**
     * @param url URL to connect to
     * @param secure Whether or not to use SSL
     */
    public Connector(String url, boolean secure) {
        this.url = url;
        this.secure = secure;
    }

    public abstract void connect() throws IOException;

    /**
     *
     * @param url URL that the connector will establish a connection for.
     * @return A connector instance
     */
    public static Connector create(final String url) {
        try {
            final URL u = new URL(url);
            final String scheme = u.getProtocol();
            switch (scheme) {
                case "http":
                case "https":
                    throw new UnsupportedOperationException("Not yet implemented");
                case "ws":
                    return new WebSocketConnector(url, false);
                case "wss":
                    return new WebSocketConnector(url, true);
                default:
                    throw new RuntimeException("Unhandled scheme: " + scheme);
            }

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
