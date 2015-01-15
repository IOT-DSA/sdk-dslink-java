package org.dsa.iot.responder.connection;

import org.dsa.iot.core.URLInfo;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import java.io.IOException;

/**
 * A common connection class to handle multiple transport protocols.
 * @author Samuel Grenier
 */
public abstract class Connector {

    private final Handler<Void> dcHandler;

    public final URLInfo url;
    public final boolean secure;

    protected final Handshake handshake;
    private boolean authenticated = false;

    /**
     * @param url URL to connect to
     * @param secure Whether or not to use SSL
     * @param dcHandler Disconnection handler, can only be called when the
     *                  remote host closes the connection or there is a network
     *                  error.
     */
    public Connector(String url, boolean secure,
                     Handler<Void> dcHandler) {
        this.url = URLInfo.parse(url);
        this.secure = secure;
        this.dcHandler = dcHandler;
        this.handshake = Handshake.generate();
    }

    /**
     * @return Whether the connection completed the initial authentication
     *         handshake.
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Writes data to the server
     * @param data Writes a response to the server
     */
    public abstract void write(String data);

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
     * It is the implementation's responsibility to call this.
     */
    protected final void connected() {
        authenticated = false;
        write(handshake.toJson().encode());
    }

    /**
     * It is the implementation's responsibility to call this. This will
     * finalize authentication. The implementation must check if the connection
     * is authenticated and call this if not.
     * @param obj Returned authentication data from server.
     */
    protected final void finalizeHandshake(JsonObject obj) {
        String dsId = obj.getString("dsId");
        String publicKey = obj.getString("publicKey");
        String wsUri = obj.getString("wsUri");
        String httpUri = obj.getString("httpUri");
        String encryptedNonce = obj.getString("encryptedNonce");
        int updateInterval = obj.getInteger("updateInterval");

        // TODO: store this data into a server information object

        authenticated = true;
    }

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
        final String protocol = URLInfo.parse(url).protocol;
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
    }
}
