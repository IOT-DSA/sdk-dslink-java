package org.dsa.iot.responder;

import lombok.Getter;
import org.dsa.iot.responder.connection.Connector;
import org.dsa.iot.responder.methods.*;
import org.dsa.iot.responder.node.Node;
import org.dsa.iot.responder.node.NodeManager;
import org.dsa.iot.responder.node.RequestTracker;
import org.dsa.iot.responder.node.SubscriptionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import static org.dsa.iot.responder.node.NodeManager.NodeStringTuple;
import static org.dsa.iot.responder.methods.Method.StreamState;

/**
 * @author Samuel Grenier
 */
@Getter
public class Responder {

    private NodeManager nodeManager;
    private SubscriptionManager subManager;
    private RequestTracker tracker;

    private Connector connector;
    private boolean connected;

    public Node createRoot(String name) {
        checkConnected();
        return nodeManager.createRootNode(name);
    }

    /**
     * Used to set the connector implementation.
     * @param connector Connector instance to use
     */
    public void setConnector(Connector connector) {
        checkConnected();
        this.connector = connector;
        subManager = new SubscriptionManager(connector);
        setSubscriptionManager(new SubscriptionManager(connector));
        setNodeManager(new NodeManager(subManager));
        setRequestTracker(new RequestTracker());
    }

    /**
     * Requires that the subscription manager is already configured.
     * Sets the node manager used for handling root nodes and node lookups.
     * @param manager Manager instance to use
     */
    public void setNodeManager(NodeManager manager) {
        checkConnector();
        checkConnected();
        nodeManager = manager;
    }

    /**
     * Requires that the connector is already configured. Used to configure how
     * subscriptions will be updated and handled.
     * @param manager Manager instance to use
     */
    public void setSubscriptionManager(SubscriptionManager manager) {
        checkConnector();
        checkConnected();
        subManager = manager;
    }

    /**
     * Used to configure how tracking is configured. The tracker is used by methods
     * who need to keep track if it should continue writing or not.
     * @param tracker Tracker instance to use.
     */
    public void setRequestTracker(RequestTracker tracker) {
        checkConnected();
        this.tracker = tracker;
    }

    /**
     * Performs the connection to the server. If attempting to connect over
     * secure communications, the SSL certificate of the server will be validated.
     * @throws IOException
     * @see #connect(boolean)
     */
    public void connect() throws IOException {
        connect(true);
    }

    /**
     * Performs the connection to the server.
     * @param sslVerify Whether to verify ssl if attempting to connect over
     *                  secure communications.
     * @throws IOException
     */
    public synchronized void connect(boolean sslVerify) throws IOException {
        checkConnected();
        checkConnector();
        connector.connect(new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                parse(event);
            }
        }, new Handler<Void>() {
            @Override
            public void handle(Void event) {
                synchronized (Responder.this) {
                    connected = false;
                }
            }
        }, sslVerify);
        connected = true;
    }

    public synchronized void disconnect() {
        connector.disconnect();
        connected = false;
    }

    /**
     * Parse the object for requests
     * @param object Object to parse
     */
    protected synchronized void parse(JsonObject object) {
        JsonArray array = object.getObject("requests").asArray();

        JsonObject top = new JsonObject();

        @SuppressWarnings("unchecked")
        Iterator<JsonObject> it = (Iterator) array.iterator();
        JsonArray responses = new JsonArray();
        for (JsonObject o; it.hasNext();) {
            o = it.next();

            Number rid = o.getNumber("rid");
            String sMethod = o.getString("method");
            String path = o.getString("path");
            NodeStringTuple node = nodeManager.getNode(path);

            Method method;
            switch (sMethod) {
                case "list":
                    method = new ListMethod(connector, node.getNode(),
                                            tracker, rid.intValue());
                    break;
                case "set":
                    method = new SetMethod(node.getNode(), node.getString());
                    break;
                case "remove":
                    method = new RemoveMethod(node.getNode(), node.getString());
                    break;
                case "invoke":
                    method = new InvokeMethod(node.getNode());
                    break;
                case "subscribe":
                    method = new SubscribeMethod(nodeManager);
                    break;
                case "unsubscribe":
                    method = new UnsubscribeMethod(nodeManager);
                    break;
                case "close":
                    method = new CloseMethod(tracker, rid.intValue());
                    break;
                default:
                    throw new RuntimeException("Unknown method");
            }

            JsonObject resp = new JsonObject();
            try {
                resp.putNumber("rid", rid);

                JsonObject updates = method.invoke(o);
                StreamState state = method.getState();

                if (state == null) {
                    throw new IllegalStateException("state");
                }
                if (state != StreamState.INITIALIZED) {
                    // This is a first response, default value is initialized if omitted
                    resp.putString("stream", state.jsonName);
                }
                if (state == StreamState.OPEN
                        || state == StreamState.INITIALIZED) {
                    tracker.track(rid.intValue());
                }
                if (updates != null && updates.size() > 0) {
                    resp.putElement("update", updates);
                }
            } catch (Exception e) {
                resp.putString("stream", StreamState.CLOSED.jsonName);

                JsonObject error = new JsonObject();
                error.putString("msg", e.getMessage());

                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                error.putString("detail", writer.toString());

                resp.putElement("error", error);
            } finally {
                responses.addElement(resp);
            }
        }

        top.putElement("responses", responses);
        connector.write(top);
    }

    private synchronized void checkConnected() {
        if (connected) {
            throw new IllegalStateException("Already connected");
        }
    }

    private void checkConnector() {
        if (connector == null) {
             throw new IllegalStateException("Connector not set");
        }
    }
}
