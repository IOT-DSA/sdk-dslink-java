package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.connection.Endpoint;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.Requester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    private static final Logger LOGGER = LoggerFactory.getLogger(DSLink.class);

    private final Endpoint endpoint;
    private final DSLinkHandler handler;
    private final NodeManager nodeManager;
    private NetworkClient client;

    /**
     * @param endpoint Connection endpoint
     * @param manager Node manager
     * @param handler Link handler
     */
    protected DSLink(Endpoint endpoint,
                     NodeManager manager,
                     DSLinkHandler handler) {
        if (endpoint == null)
            throw new NullPointerException("endpoint");
        else if (manager == null)
            throw new NullPointerException("manager");
        else if (handler == null)
            throw new NullPointerException("handler");
        this.endpoint = endpoint;
        this.nodeManager = manager;
        this.handler = handler;
        endpoint.setClientConnectedHandler(new Handler<NetworkClient>() {
            @Override
            public synchronized void handle(NetworkClient event) {
                if (client != null) {
                    // Will only happen in a server side endpoint
                    LOGGER.warn("Client already configured");
                    event.close();
                } else {
                    client = event;
                    defaultDataHandler();
                    DSLink.this.handler.onConnected(DSLink.this);
                }
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
     * @return Requester of the singleton client
     */
    public Requester getRequester() {
        return client.getRequester();
    }

    /**
     * @return Node manager of the link.
     */
    public NodeManager getNodeManager() {
        return nodeManager;
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

    /**
     * Sets the default data handler to the remote endpoint.
     */
    protected void defaultDataHandler() {
        client.setDataHandler(new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                Requester requester = client.getRequester();
                if (requester != null) {
                    JsonArray array = event.getArray("responses");
                    if (array != null) {
                        handleResponses(requester, array);
                    }
                }
            }
        });
    }

    /**
     * @param requester Requester of the client
     * @param array Array of responses
     */
    protected void handleResponses(Requester requester, JsonArray array) {
        for (Object object : array) {
            requester.parseResponse((JsonObject) object);
        }
    }
}
