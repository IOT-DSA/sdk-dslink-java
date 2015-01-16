package org.dsa.iot.responder.connection;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.dsa.iot.core.URLInfo;
import org.dsa.iot.responder.connection.handshake.HandshakeServer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.io.UnsupportedEncodingException;

/**
 * @author Samuel Grenier
 */
public abstract class Connector {

    public final URLInfo dataEndpoint;
    public final HandshakeServer hs;

    public Connector(URLInfo dataEndpoint, HandshakeServer hs) {
        this.dataEndpoint = dataEndpoint;
        this.hs = hs;
    }

    /**
     * Connects to the server based on the implementation.
     * @param data Handles incoming JSON parsed data
     * @param dcHandler Disconnection handler. If {@link #disconnect} is called,
     *                  this handler will not be called.
     * @param sslVerify Whether to validate the server SSL certificate if
     *                  attempting to connect over secure communications
     */
    public abstract void connect(final Handler<JsonObject> data,
                                 final Handler<Void> dcHandler,
                                 final boolean sslVerify);

    /**
     * Forcefully disconnects the client from the server.
     */
    public abstract void disconnect();

    /**
     * @return A full path with an attached query string
     */
    protected String getPath() {
        StringBuilder query = new StringBuilder(dataEndpoint.path);
        try { // Auth parameter
            query.append("?auth=");

            byte[] salt = getSalt().getBytes("UTF-8");
            byte[] nonce = hs.nonce;

            Buffer buffer = new Buffer(salt.length + nonce.length);
            buffer.appendBytes(salt);
            buffer.appendBytes(nonce);

            SHA256.Digest digest = new SHA256.Digest();
            byte[] output = digest.digest(buffer.getBytes());
            query.append(Base64.encodeBytes(output, Base64.URL_SAFE));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return query.toString();
    }

    protected String getSalt() {
        return hs.salt;
    }

    /**
     *
     * @param url URL to connect to.
     * @param hs Retrieved information about the handshake from the server.
     * @param type If type is {@link ConnectionType#HTTP}
     *             or {@link ConnectionType#WS} then the URL must be a handshake
     *             connection initiation endpoint, not the actual data URL.
     * @return A connector instance
     */
    public static Connector create(String url, HandshakeServer hs, ConnectionType type) {
        switch (type) {
            case SOCKET:
                throw new UnsupportedOperationException("Sockets not implemented yet");
            case HTTP:
                throw new UnsupportedOperationException("HTTP not implemented yet");
            case WS:
                return new WebSocketConnector(URLInfo.parse(url), hs);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }
}
