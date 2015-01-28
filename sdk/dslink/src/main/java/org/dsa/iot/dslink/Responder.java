package org.dsa.iot.dslink;

import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.Connector;
import org.dsa.iot.dslink.methods.*;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.util.Linkable;
import org.dsa.iot.dslink.util.ResponseTracker;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import static org.dsa.iot.dslink.node.NodeManager.NodeStringTuple;

/**
 * @author Samuel Grenier
 */
@Getter
public class Responder extends Linkable {

    private NodeManager nodeManager;
    private ResponseTracker tracker;

    public Responder() {
        this(new ResponseTracker());
    }

    public Responder(ResponseTracker tracker) {
        this.tracker = tracker;
    }

    public Node createRoot(String name) {
        checkConnected();
        return nodeManager.createRootNode(name);
    }

    /**
     * Requires that the subscription manager is already configured.
     * Sets the node manager used for handling root nodes and node lookups.
     * @param manager Manager instance to use
     */
    public void setNodeManager(NodeManager manager) {
        checkConnected();
        nodeManager = manager;
    }

    /**
     * When setting the connector, the node manager will be overwritten to
     * default instances.
     * @param connector Connector to be set.
     */
    @Override
    public void setConnector(Connector connector) {
        super.setConnector(connector);
        setNodeManager(new NodeManager(new SubscriptionManager(connector)));
    }

    /**
     * @param requests The requests the other endpoint wants
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized void parse(JsonArray requests) {
        Iterator<JsonObject> it = (Iterator) requests.iterator();
        JsonArray responses = new JsonArray();
        for (JsonObject o; it.hasNext();) {
            o = it.next();

            Number rid = o.getNumber("rid");
            String sMethod = o.getString("method");
            String path = o.getString("path");

            JsonObject resp = new JsonObject();
            try {
                resp.putNumber("rid", rid);
                NodeStringTuple node = null;
                if (path != null) {
                    node = nodeManager.getNode(path);
                }

                Method method = getMethod(sMethod, rid.intValue(), node);
                JsonArray updates = method.invoke(o);
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
                handleInvocationError(resp, e);
            } finally {
                responses.addElement(resp);
            }
        }

        JsonObject top = new JsonObject();
        top.putElement("responses", responses);
        getConnector().write(top);
    }

    protected Method getMethod(@NonNull String name, int rid,
                               NodeStringTuple tuple) {
        switch (name) {
            case "list":
                return new ListMethod(getConnector(), tuple.getNode(),
                                        tracker, rid);
            case "set":
                return new SetMethod(tuple.getNode(), tuple.getString());
            case "remove":
                return new RemoveMethod(tuple.getNode(), tuple.getString());
            case "invoke":
                return new InvokeMethod(tuple.getNode());
            case "subscribe":
                return new SubscribeMethod(nodeManager);
            case "unsubscribe":
                return new UnsubscribeMethod(nodeManager);
            case "close":
                return new CloseMethod(tracker, rid);
            default:
                throw new RuntimeException("Unknown method");
        }
    }

    protected void handleInvocationError(JsonObject resp, Exception e) {
        e.printStackTrace(System.err);
        resp.putString("stream", StreamState.CLOSED.jsonName);

        JsonObject error = new JsonObject();
        error.putString("msg", e.getMessage());

        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        error.putString("detail", writer.toString());

        resp.putElement("error", error);
    }
}
