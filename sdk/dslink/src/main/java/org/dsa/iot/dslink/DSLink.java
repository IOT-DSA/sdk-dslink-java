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

import static org.dsa.iot.dslink.connection.DataHandler.DataReceived;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    private final SubscriptionManager manager = new SubscriptionManager(this);
    private final DSLinkHandler linkHandler;
    private final NodeManager nodeManager;
    private final Requester requester;
    private final Responder responder;

    private SerializationManager serialManager;
    private DataHandler dataHandler;

    /**
     * @param linkHandler DSLink dataHandler
     * @param isReqOrResp {@code true} for requester, otherwise {@code false}
     *                    for a responder.
     */
    protected DSLink(DSLinkHandler linkHandler,
                     boolean isReqOrResp) {
        if (linkHandler == null)
            throw new NullPointerException("linkHandler");

        this.linkHandler = linkHandler;
        if (isReqOrResp) {
            requester = new Requester(linkHandler);
            requester.setDSLink(this);
            responder = null;
            nodeManager = new NodeManager(requester, "node");
        } else {
            responder = new Responder(linkHandler);
            responder.setDSLink(this);
            requester = null;
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

    /**
     * @return The serialization manager this instance is attached to.
     */
    public SerializationManager getSerialManager() {
        return serialManager;
    }

    /**
     * @return The link handler this instance is attached to.
     */
    public DSLinkHandler getLinkHandler() {
        return linkHandler;
    }

    /**
     * Sets a new writer for the link to utilize.
     *
     * @param handler New data handler to set.
     */
    public void setWriter(DataHandler handler) {
        this.dataHandler = handler;
    }

    /**
     * The writer must never be cached as it can change.
     *
     * @return The writer to write responses to a remote endpoint.
     */
    public DataHandler getWriter() {
        return dataHandler;
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
            getWriter().setRespHandler(new Handler<DataReceived>() {
                @Override
                public void handle(DataReceived event) {
                    // TODO: ack handling
                    JsonArray array = event.getData();
                    for (Object object : array) {
                        JsonObject json = (JsonObject) object;
                        DSLink.this.requester.parse(json);
                    }
                }
            });

            getWriter().setCloseHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    DSLink.this.requester.clearSubscriptions();
                }
            });
        }
        if (responder) {
            getWriter().setReqHandler(new Handler<DataReceived>() {
                @Override
                public void handle(DataReceived event) {
                    final JsonArray data = event.getData();
                    List<JsonObject> responses = new LinkedList<>();
                    for (Object object : data) {
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

                    Integer msgId = event.getMsgId();
                    getWriter().writeRequestResponses(msgId, responses);
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
