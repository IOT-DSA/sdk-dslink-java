package org.dsa.iot.dslink;

import static org.dsa.iot.dslink.connection.DataHandler.DataReceived;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.serializer.SerializationManager;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    private static final Logger LOGGER = LoggerFactory.getLogger(DSLink.class);
    private final boolean isResponder;
    private final DSLinkHandler linkHandler;
    private final SubscriptionManager manager;
    private final NodeManager nodeManager;
    private final Requester requester;
    private final Responder responder;
    private final String path;
    private DataHandler dataHandler;
    private SerializationManager serialManager;

    /**
     * @param linkHandler DSLink dataHandler
     * @param isReqOrResp {@code true} for requester, otherwise {@code false}
     *                    for a responder.
     * @param path        Path of the {@code DSLink} on the broker.
     */
    protected DSLink(DSLinkHandler linkHandler,
                     boolean isReqOrResp,
                     String path) {
        if (linkHandler == null) {
            throw new NullPointerException("linkHandler");
        }

        this.linkHandler = linkHandler;
        this.path = path;
        isResponder = !isReqOrResp;
        manager = new SubscriptionManager(this);
        if (isReqOrResp) {
            requester = new Requester(linkHandler);
            requester.setDSLink(this);
            responder = null;
            nodeManager = linkHandler.createRequesterNodeManager(requester, "node");
        } else {
            responder = new Responder(linkHandler);
            responder.setDSLink(this);
            requester = null;
            nodeManager = linkHandler.createResponderNodeManager(responder, "node");
        }
    }

    /**
     * @return The link handler this instance is attached to.
     */
    public DSLinkHandler getLinkHandler() {
        return linkHandler;
    }

    /**
     * @return Node manager of the link.
     */
    public NodeManager getNodeManager() {
        return nodeManager;
    }

    /**
     * @return The path of the link on the broker.
     */
    public String getPath() {
        return path;
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
     * @return The serialization manager this instance is attached to.
     */
    public SerializationManager getSerialManager() {
        return serialManager;
    }

    /**
     * @return Subscription manager
     */
    public SubscriptionManager getSubscriptionManager() {
        return manager;
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
     * @return Whether the DSLink is connected or not.
     */
    public boolean isConnected() {
        return dataHandler != null && dataHandler.isConnected();
    }

    public boolean isResponder() {
        return isResponder;
    }

    /**
     * Sets the default data handler to the remote endpoint.
     *
     * @param requester Whether to handle responses.
     * @param responder Whether to handle requests.
     */
    public void setDefaultDataHandlers(boolean requester, boolean responder) {
        if (requester) {
            getWriter().setRespHandler(new Handler<DataReceived>() {
                @Override
                public void handle(DataReceived event) {
                    JsonArray array = event.getData();
                    for (Object object : array) {
                        try {
                            JsonObject json = (JsonObject) object;
                            DSLink.this.requester.parse(json);
                        } catch (RuntimeException e) {
                            LOGGER.error("Failed to parse json", e);
                        }
                    }
                    getWriter().writeAck(event.getMsgId());
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
                            Integer rid = json.get("rid");
                            if (rid != null) {
                                resp.put("rid", rid);
                            }
                            resp.put("stream", StreamState.CLOSED.getJsonName());

                            JsonObject err = new JsonObject();
                            err.put("msg", e.getMessage());
                            { // Build stack trace
                                StringWriter writer = new StringWriter();
                                e.printStackTrace(new PrintWriter(writer));
                                err.put("detail", writer.toString());
                            }
                            resp.put("error", err);
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
     * Sets a new writer for the link to utilize.
     *
     * @param handler New data handler to set.
     */
    public void setWriter(DataHandler handler) {
        this.dataHandler = handler;
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
