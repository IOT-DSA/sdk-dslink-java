package org.dsa.iot.dslink;

import lombok.Getter;
import org.dsa.iot.dslink.connection.Connector;
import org.dsa.iot.dslink.methods.*;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.RequestTracker;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.util.Linkable;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import static org.dsa.iot.dslink.methods.Method.StreamState;
import static org.dsa.iot.dslink.node.NodeManager.NodeStringTuple;

/**
 * @author Samuel Grenier
 */
@Getter
public class Responder implements Linkable {

    private NodeManager nodeManager;
    private SubscriptionManager subManager;
    private RequestTracker tracker;

    private Connector connector;

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
     * Requires that the connector is already configured. Used to configure how
     * subscriptions will be updated and handled.
     * @param manager Manager instance to use
     */
    public void setSubscriptionManager(SubscriptionManager manager) {
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
     * @param requests The requests the other endpoint wants
     */
    @Override
    public synchronized void parse(JsonArray requests) {
        JsonObject top = new JsonObject();

        @SuppressWarnings("unchecked")
        Iterator<JsonObject> it = (Iterator) requests.iterator();
        JsonArray responses = new JsonArray();
        for (JsonObject o; it.hasNext();) {
            o = it.next();

            Number rid = o.getNumber("rid");
            String sMethod = o.getString("method");
            String path = o.getString("path");
            NodeStringTuple node = nodeManager.getNode(path);

            JsonObject resp = new JsonObject();
            try {
                resp.putNumber("rid", rid);

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

    /**
     * When setting the connector, the subscription manager, node manager, and
     * request tracker will be overwritten to default instances.
     * @param connector Connector to be set.
     */
    @Override
    public void setConnector(Connector connector) {
        this.connector = connector;
        setSubscriptionManager(new SubscriptionManager(connector));
        setNodeManager(new NodeManager(subManager));
        setRequestTracker(new RequestTracker());
    }

    private synchronized void checkConnected() {
        if (connector.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
    }
}
