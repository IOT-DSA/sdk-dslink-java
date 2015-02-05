package org.dsa.iot.dslink.connection;

import com.google.common.eventbus.EventBus;
import lombok.*;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.dsa.iot.core.URLInfo;
import org.dsa.iot.dslink.connection.connector.client.WebSocketConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.io.UnsupportedEncodingException;

/**
 * Used for handling client connections to servers.
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public abstract class ClientConnector {

    @NonNull
    private final EventBus bus;

    @NonNull
    private final URLInfo dataEndpoint;

    @NonNull
    private final HandshakePair pair;

    /**
     * Connects to the server based on the implementation.
     * @param sslVerify Whether to validate the server SSL certificate if
     *                  attempting to connect over secure communications
     */
    public abstract void connect(final boolean sslVerify);

    /**
     * Forcefully disconnects the client from the server.
     */
    public abstract void disconnect();

    /**
     * Writes the data to the server.
     * @param obj Object to be encoded
     */
    public abstract void write(JsonObject obj);

    public abstract boolean isConnected();

    /**
     * @return A full path with an attached query string
     */
    protected String getPath() {
        StringBuilder query = new StringBuilder(dataEndpoint.path);
        try { // Auth parameter
            String uri = pair.getServer().getWsUri();
            if (uri.startsWith("/"))
                uri = uri.substring(1);
            if (!dataEndpoint.path.equals("/"))
                query.append("/");
            query.append(uri);
            query.append("?auth=");

            byte[] salt = getSalt().getBytes("UTF-8");
            byte[] sharedSecret = pair.getServer().getSharedSecret();

            Buffer buffer = new Buffer(salt.length + sharedSecret.length);
            buffer.appendBytes(salt);
            buffer.appendBytes(sharedSecret);

            SHA256.Digest digest = new SHA256.Digest();
            byte[] output = digest.digest(buffer.getBytes());

            String encoded = Base64.encodeBytes(output, Base64.URL_SAFE);
            query.append(encoded.substring(0, encoded.length() - 1));

            query.append("&dsId=");
            query.append(pair.getClient().getDsId());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return query.toString();
    }

    protected String getSalt() {
        return pair.getServer().getSalt();
    }

    /**
     *
     * @param url URL to connect to.
     * @param pair The handshake authentication information
     * @param type If type is {@link ConnectionType#HTTP}
     *             or {@link ConnectionType#WS} then the URL must be a handshake
     *             connection initiation endpoint, not the actual data URL.
     * @return A connector instance
     */
    public static ClientConnector create(EventBus bus, String url,
                                         HandshakePair pair, ConnectionType type) {
        switch (type) {
            case SOCKET:
                throw new UnsupportedOperationException("Sockets not implemented yet");
            case HTTP:
                throw new UnsupportedOperationException("HTTP not implemented yet");
            case WS:
                return new WebSocketConnector(bus, URLInfo.parse(url), pair);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }
}
