package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.Requester;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    private final NodeManager nodeManager;
    private final Requester requester;
    private NetworkClient client;

    /**
     * @param handler DSLink handler
     * @param client Initialized client endpoint
     */
    protected DSLink(DSLinkHandler handler,
                     NetworkClient client) {
        if (client == null)
            throw new NullPointerException("client");
        this.client = client;
        this.nodeManager = new NodeManager();
        if (client.isRequester()) {
            requester = new Requester(handler);
            requester.setDSLink(this);
        } else {
            requester = null;
        }
    }

    /**
     * @return The network client to write data to.
     */
    public NetworkClient getClient() {
        return client;
    }

    /**
     * @return Requester of the singleton client, can be null
     */
    public Requester getRequester() {
        return requester;
    }

    /**
     * @return Node manager of the link.
     */
    public NodeManager getNodeManager() {
        return nodeManager;
    }

    /**
     * Sets the default data handler to the remote endpoint.
     */
    public void setDefaultDataHandlers() {
        if (client.isRequester()) {
            client.setResponseDataHandler(new Handler<JsonArray>() {
                @Override
                public void handle(JsonArray event) {
                    for (Object object : event) {
                        JsonObject json = (JsonObject) object;
                        requester.parseResponse(json);
                    }
                }
            });
        } else if (client.isResponder()) {
            throw new UnsupportedOperationException("responder");
        }
    }
}
