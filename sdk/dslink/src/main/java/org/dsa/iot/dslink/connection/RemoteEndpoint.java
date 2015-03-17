package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.util.URLInfo;

/**
 * Common interface for handling remote endpoints.
 * @author Samuel Grenier
 */
public abstract class RemoteEndpoint implements NetworkClient, Endpoint {

    private LocalHandshake localHandshake;
    private RemoteHandshake remoteHandshake;
    private URLInfo endpoint;

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
}
