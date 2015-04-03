package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    private final SubscriptionManager manager = new SubscriptionManager(this);
    private final NodeManager nodeManager;
    private final Requester requester;
    private final Responder responder;
    private NetworkClient client;

    /**
     * @param handler DSLink handler
     * @param client Initialized client endpoint
     * @param isRequester Whether to initialize a requester, otherwise
     *                    a responder is initialized. The initialize
     *                    param must be true.
     * @param initialize Whether to initialize a requester or link
     */
    protected DSLink(DSLinkHandler handler,
                     NetworkClient client,
                     boolean isRequester,
                     boolean initialize) {
        if (client == null)
            throw new NullPointerException("client");
        this.client = client;

        if (initialize && isRequester) {
            requester = new Requester(handler);
            responder = null;
            requester.setDSLink(this);
            nodeManager = new NodeManager(requester, "node");
        } else if (initialize) {
            requester = null;
            responder = new Responder(handler);
            responder.setDSLink(this);
            nodeManager = new NodeManager(responder, "node");
        } else {
            requester = null;
            responder = null;
            nodeManager = null;
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
     * @return Subscription manager
     */
    public SubscriptionManager getSubscriptionManager() {
        return manager;
    }

    /**
     * Sets the default data handler to the remote endpoint.
     * @param requester Whether to handle responses.
     * @param responder Whether to handle requests.
     */
    public void setDefaultDataHandlers(boolean requester, boolean responder) {
        if (requester) {
            client.setResponseDataHandler(new Handler<JsonArray>() {
                @Override
                public void handle(JsonArray event) {
                    for (Object object : event) {
                        JsonObject json = (JsonObject) object;
                        DSLink.this.requester.parse(json);
                    }
                }
            });
        }
        if (responder) {
            client.setRequestDataHandler(new Handler<JsonArray>() {
                @Override
                public void handle(JsonArray event) {
                    List<JsonObject> responses = new LinkedList<>();
                    for (Object object : event) {
                        JsonObject json = (JsonObject) object;
                        try {
                            JsonObject resp = DSLink.this.responder.parse(json);
                            responses.add(resp);
                        } catch (Exception e) {
                            JsonObject resp = new JsonObject();
                            Integer rid = json.getInteger("rid");
                            if (rid != null) {
                                resp.putNumber("rid", rid);
                            }
                            resp.putString("stream", StreamState.CLOSED.getJsonName());

                            JsonObject err = new JsonObject();
                            resp.putString("msg", e.getMessage());
                            { // Build stack trace
                                StringWriter writer = new StringWriter();
                                e.printStackTrace(new PrintWriter(writer));
                                resp.putString("detail", writer.toString());
                            }
                            resp.putObject("error", err);
                            responses.add(resp);
                        }
                    }

                    client.writeResponses(responses);
                }
            });
        }
    }
}
