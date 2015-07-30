package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.serializer.SerializationManager;
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

    private SerializationManager serialManager;
    private DataHandler handler;

    /**
     * @param linkHandler DSLink handler
     * @param isRequester Whether to initialize a requester.
     * @param isResponder Whether to initialize a responder.
     */
    protected DSLink(DSLinkHandler linkHandler,
                     boolean isRequester,
                     boolean isResponder) {
        if (linkHandler == null)
            throw new NullPointerException("linkHandler");
        else if (isRequester && isResponder) {
            String err = "DSLink instance cannot be a requester and responder";
            throw new RuntimeException(err);
        }

        if (isRequester) {
            requester = new Requester(linkHandler);
            requester.setDSLink(this);
        } else {
            requester = null;
        }

        if (isResponder) {
            responder = new Responder(linkHandler);
            responder.setDSLink(this);
        } else {
            responder = null;
        }

        if (isRequester) {
            nodeManager = new NodeManager(requester, "node");
        } else {
            nodeManager = new NodeManager(responder, "node");
        }
    }

    /**
     * Sets the serialization manager on the dslink.
     *
     * @param manager Serialization manager
     */
    public void setSerialManager(SerializationManager manager) {
        if (this.serialManager != null) {
            this.serialManager.stop();
        }
        this.serialManager = manager;
    }

    public SerializationManager getSerialManager() {
        return serialManager;
    }

    /**
     * Sets a new writer for the link to utilize.
     *
     * @param handler New handler to set.
     */
    public void setWriter(DataHandler handler) {
        this.handler = handler;
    }

    /**
     * The writer must never be cached as it can change.
     *
     * @return The writer to write responses to a remote endpoint.
     */
    public DataHandler getWriter() {
        return handler;
    }

    /**
     * @return Requester of the link, can be {@code null}.
     */
    public Requester getRequester() {
        return requester;
    }

    /**
     * @return Responder of the link, can be {@code null}.
     */
    public Responder getResponder() {
        return responder;
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
            getWriter().setRespHandler(new Handler<JsonArray>() {
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
            getWriter().setReqHandler(new Handler<JsonArray>() {
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

                    getWriter().writeResponses(responses);
                }
            });
        }
    }

    /**
     * Stops the DSLink.
     */
    public void stop() {
        SerializationManager manager = getSerialManager();
        if (manager != null) {
            manager.stop();
        }
    }
}
