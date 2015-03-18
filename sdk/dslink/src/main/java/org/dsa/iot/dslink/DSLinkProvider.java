package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.Endpoint;
import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.node.NodeManager;
import org.vertx.java.core.Handler;

/**
 * Provides DSLinks as soon as a client connects to the server or vice versa.
 * @author Samuel Grenier
 */
public class DSLinkProvider {

    private final Endpoint endpoint;
    private final DSLinkHandler handler;

    public DSLinkProvider(Endpoint endpoint, DSLinkHandler handler) {
        if (endpoint == null)
            throw new NullPointerException("endpoint");
        else if (handler == null)
            throw new NullPointerException("handler");
        this.endpoint = endpoint;
        this.handler = handler;
    }

    /**
     * Sets the default endpoint handler. Override if a custom DSLink
     * implementation needs to be provided.
     * @see DSLinkHandler#onConnected
     */
    public void setDefaultEndpointHandler() {
        endpoint.setClientConnectedHandler(new Handler<NetworkClient>() {
            @Override
            public synchronized void handle(NetworkClient event) {
                NodeManager manager = new NodeManager();
                DSLink link = new DSLink(event, manager);
                link.setDefaultDataHandler();
                handler.onConnected(link);
            }
        });
    }

    /**
     * @see RemoteEndpoint#activate()
     */
    public void start() {
        endpoint.activate();
    }

    /**
     * @see RemoteEndpoint#deactivate()
     */
    public void stop() {
        endpoint.deactivate();
    }

    /**
     * Blocks the thread indefinitely while the endpoint is active or connected
     * to a host. This will automatically unblock when the endpoint becomes
     * inactive or disconnects, allowing the thread to proceed execution. Typical
     * usage is to call {@code sleep} in the main thread to prevent the application
     * from terminating abnormally.
     */
    public void sleep() {
        try {
            while (endpoint.isBecomingActive() || endpoint.isActive()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
