package org.dsa.iot.dslink.connection;

import org.vertx.java.core.Handler;

/**
 * Common endpoint API
 *
 * @author Samuel Grenier
 */
public interface Endpoint {

    /**
     * Handler is called when a client is connected to the internal server, or
     * the link has connected to a remote endpoint.
     *
     * @param handler Connected client handler
     */
    void setClientConnectedHandler(Handler<NetworkClient> handler);

    /**
     * Activate the connection to the remote endpoint or start a server
     * depending on the implementation.
     */
    void activate();

    /**
     * Deactivate connection to the remote endpoint or stop a server
     * depending on the implementation.
     */
    void deactivate();
}
