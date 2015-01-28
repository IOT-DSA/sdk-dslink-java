package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.Connector;
import org.dsa.iot.dslink.methods.Method;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.requests.ListRequest;
import org.dsa.iot.dslink.requests.Request;
import org.dsa.iot.dslink.responses.ListResponse;
import org.dsa.iot.dslink.responses.Response;
import org.dsa.iot.dslink.util.Linkable;
import org.dsa.iot.dslink.util.RequestTracker;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Iterator;

/**
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private NodeManager nodeManager;
    private RequestTracker tracker;

    public Requester() {
        this(new RequestTracker());
    }

    public Requester(RequestTracker tracker) {
        this.tracker = tracker;
    }

    public void sendRequest(Request req) {
        checkConnected();

        JsonObject obj = new JsonObject();
        obj.putNumber("rid", tracker.track(req));
        obj.putString("method", req.getName());
        req.addJsonValues(obj);

        JsonObject reqs = new JsonObject();
        reqs.putObject("requests", obj);
        getConnector().write(reqs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parse(JsonArray responses) {
        try {
            Iterator<JsonObject> it = (Iterator) responses.iterator();
            for (JsonObject o; it.hasNext();) {
                o = it.next();

                int rid = o.getNumber("rid").intValue();
                if (rid != 0) {
                    // Response

                    Request request = tracker.getRequest(rid);
                    String state = o.getString("state");
                    if (Method.StreamState.CLOSED.jsonName.equals(state)) {
                        tracker.untrack(rid);
                    }

                    Response<?> resp;
                    switch (o.getString("method")) {
                        case "list":
                            resp = new ListResponse((ListRequest) request, nodeManager);
                            break;
                        case "set":
                            throw new UnsupportedOperationException(); // TODO
                            //break;
                        case "remove":
                            throw new UnsupportedOperationException(); // TODO
                            //break;
                        case "invoke":
                            throw new UnsupportedOperationException(); // TODO
                            //break;
                        case "subscribe":
                            throw new UnsupportedOperationException(); // TODO
                            //break;
                        case "unsubscribe":
                            throw new UnsupportedOperationException(); // TODO
                            //break;
                        case "close":
                            throw new UnsupportedOperationException(); // TODO
                            //break;
                        default:
                            throw new RuntimeException("Unknown method");
                    }
                    resp.populate(o.getArray("update"));
                    // TODO: get the data to the API that wants it
                } else {
                    // Subscription update
                    // TODO
                }
            }
        } catch (Exception e) {
            // Error handler data
            e.printStackTrace(System.err);
        }
    }

    public void setRequestTracker(RequestTracker tracker) {
        checkConnected();
        this.tracker = tracker;
    }

    /**
     * When setting the connector, the subscription manager, node manager, and
     * request tracker will be overwritten to default instances.
     * @param connector Connector to be set.
     */
    @Override
    public void setConnector(Connector connector) {
        super.setConnector(connector);
        nodeManager = new NodeManager(new SubscriptionManager(connector));
    }
}
