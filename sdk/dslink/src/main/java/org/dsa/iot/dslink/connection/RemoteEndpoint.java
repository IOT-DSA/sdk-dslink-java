package org.dsa.iot.dslink.connection;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.UrlBase64;
import org.vertx.java.core.buffer.Buffer;

import java.io.UnsupportedEncodingException;

/**
 * Common interface for handling remote endpoints.
 *
 * @author Samuel Grenier
 */
public abstract class RemoteEndpoint implements NetworkClient, Endpoint {

    private LocalHandshake localHandshake;
    private RemoteHandshake remoteHandshake;
    private URLInfo endpoint;

    /**
     * Initializes the connector after all the data variables have been
     * configured.
     */
    public void init() {
    }

    @Override
    public final void close() {
        deactivate();
    }

    /**
     * @param handshake Local handshake information to set.
     */
    public void setLocalHandshake(LocalHandshake handshake) {
        this.localHandshake = handshake;
    }

    /**
     * @return Local handshake information
     */
    public LocalHandshake getLocalHandshake() {
        return localHandshake;
    }

    /**
     * @param handshake Remote handshake set after the authentication to the
     *                  auth endpoint was successful.
     */
    public void setRemoteHandshake(RemoteHandshake handshake) {
        this.remoteHandshake = handshake;
    }

    /**
     * @return Remote handshake.
     */
    public RemoteHandshake getRemoteHandshake() {
        return remoteHandshake;
    }

    /**
     * @param endpoint Handshake URL endpoint
     */
    public void setEndpoint(URLInfo endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return Handshake URL endpoint
     */
    public URLInfo getEndpoint() {
        return endpoint;
    }

    public String getUri() {
        RemoteHandshake handshake = remoteHandshake;
        String uri = handshake.getWsUri() + "?auth=";
        try {
            byte[] salt = handshake.getSalt().getBytes("UTF-8");
            byte[] sharedSecret = handshake.getRemoteKey().getSharedSecret();

            Buffer buffer = new Buffer(salt.length + sharedSecret.length);
            buffer.appendBytes(salt);
            buffer.appendBytes(sharedSecret);

            SHA256.Digest sha = new SHA256.Digest();
            byte[] digested = sha.digest(buffer.getBytes());
            String encoded = UrlBase64.encode(digested);
            uri += encoded + "&dsId=" + getLocalHandshake().getDsId();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return uri;
    }
}
