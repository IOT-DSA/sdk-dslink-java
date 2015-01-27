package org.dsa.iot.dslink;

import org.dsa.iot.dslink.node.RequestTracker;
import org.dsa.iot.dslink.requests.Request;
import org.dsa.iot.dslink.util.Linkable;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Iterator;

/**
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private RequestTracker tracker;

    public Requester() {
        this.tracker = new RequestTracker();
    }

    public void sendRequest(Request req) {
        checkConnected();

        JsonObject obj = new JsonObject();
        obj.putNumber("rid", tracker.getNextID());
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

                Number rid = o.getNumber("rid");
                if (rid.intValue() != 0) {
                    // Response
                    tracker.untrack(rid.intValue());

                    switch (o.getString("method")) {
                        case "list":
                            throw new UnsupportedOperationException(); // TODO
                        case "set":
                            throw new UnsupportedOperationException(); // TODO
                        case "remove":
                            throw new UnsupportedOperationException(); // TODO
                        case "invoke":
                            throw new UnsupportedOperationException(); // TODO
                        case "subscribe":
                            throw new UnsupportedOperationException(); // TODO
                        case "unsubscribe":
                            throw new UnsupportedOperationException(); // TODO
                        case "close":
                            throw new UnsupportedOperationException(); // TODO
                        default:
                            throw new RuntimeException("Unknown method");
                    }
                } else {
                    // Subscription update
                }
            }
        } catch (Exception e) {
            // Error handler data
            e.printStackTrace(System.err);
        }
    }
}
