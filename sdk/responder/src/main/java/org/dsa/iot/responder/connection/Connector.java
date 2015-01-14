package org.dsa.iot.responder.connection;

import org.dsa.iot.responder.Responder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A common connection class to handle multiple transport protocols.
 * @author Samuel Grenier
 */
public abstract class Connector {

    private final Handler<Void> dcHandler;

    public final String url;
    public final boolean secure;

    /**
     * @param url URL to connect to
     * @param secure Whether or not to use SSL
     * @param dcHandler Disconnection handler, can only be called when the
     *                  remote host closes the connection or there is a network
     *                  error.
     */
    public Connector(String url, boolean secure,
                     Handler<Void> dcHandler) {
        this.url = url;
        this.secure = secure;
        this.dcHandler = dcHandler;
    }

    /**
     * Connects to the server.
     * @param parser parses the incoming data from the server
     * @throws IOException
     */
    public abstract void connect(Handler<JsonObject> parser) throws IOException;

    /**
     * Closes the connection to the server.
     */
    public abstract void disconnect();

    /**
     * Called when a client is disconnected due to network error or the remote
     * host closed the connection.
     */
    protected final void disconnected() {
        if (dcHandler != null) {
            dcHandler.handle(null);
        }
    }

    /**
     * Factory method used to create a default connector implementation based
     * on the URL protocol.
     * @return A connector instance
     * @see #Connector
     */
    public static Connector create(String url, Handler<Void> dcHandler) {
        try {
            final URL u = new URL(url);
            final String protocol = u.getProtocol();
            switch (protocol) {
                case "http":
                case "https":
                    throw new UnsupportedOperationException("Not yet implemented");
                case "ws":
                    return new WebSocketConnector(url, false, dcHandler);
                case "wss":
                    return new WebSocketConnector(url, true, dcHandler);
                default:
                    throw new RuntimeException("Unhandled protocol: " + protocol);
            }

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an initialized set of handshake headers ready for use
     * @return Handshake headers in a map
     */
    protected static MultiMap getHandshake() {
        return null; // TODO
    }
}
