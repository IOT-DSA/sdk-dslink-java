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
    private NetworkClient client;

    /**
     * @param client Initialized client
     * @param manager  Node manager
     */
    protected DSLink(NetworkClient client,
                     NodeManager manager) {
        if (client == null)
            throw new NullPointerException("client");
        else if (manager == null)
            throw new NullPointerException("manager");
        this.client = client;
        this.nodeManager = manager;
    }

    /**
     * @return Requester of the singleton client, can be null
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
     * Sets the default data handler to the remote endpoint.
     */
    public void setDefaultDataHandler() {
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
     * @param array     Array of responses
     */
    protected void handleResponses(Requester requester, JsonArray array) {
        for (Object object : array) {
            requester.parseResponse((JsonObject) object);
        }
    }
}
