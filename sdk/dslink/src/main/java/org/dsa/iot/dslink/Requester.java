package org.dsa.iot.dslink;

import com.google.common.eventbus.EventBus;
import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.requests.*;
import org.dsa.iot.dslink.responses.*;
import org.dsa.iot.dslink.util.Linkable;
import org.dsa.iot.dslink.util.RequestTracker;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Iterator;

/**
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private RequestTracker tracker;
    private Handler<Response<?>> responseHandler;

    public Requester(EventBus bus) {
        this(bus, new RequestTracker());
    }

    public Requester(EventBus bus, RequestTracker tracker) {
        super(bus);
        this.tracker = tracker;
    }

    public void sendRequest(@NonNull Request req) {
        checkConnected();

        val obj = new JsonObject();
        obj.putNumber("rid", tracker.track(req));
        obj.putString("method", req.getName());
        req.addJsonValues(obj);

        val reqs = new JsonObject();
        reqs.putObject("requests", obj);
        getConnector().write(reqs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parse(JsonArray responses) {
        try {
            val it = responses.iterator();
            for (JsonObject o; it.hasNext();) {
                o = (JsonObject) it.next();

                int rid = o.getNumber("rid").intValue();
                Request request = tracker.getRequest(rid);
                Response<?> resp;
                if (rid != 0) {
                    // Response
                    String state = o.getString("state");
                    if (StreamState.CLOSED.jsonName.equals(state)) {
                        tracker.untrack(rid);
                    }

                    switch (o.getString("method")) {
                        case "list":
                            resp = new ListResponse((ListRequest) request, getManager());
                            break;
                        case "set":
                            resp = new SetResponse((SetRequest) request);
                            break;
                        case "remove":
                            resp = new RemoveResponse((RemoveRequest) request);
                            break;
                        case "invoke":
                            resp = new InvokeResponse((InvokeRequest) request);
                            break;
                        case "subscribe":
                            resp = new SubscribeResponse((SubscribeRequest) request);
                            break;
                        case "unsubscribe":
                            resp = new UnsubscribeResponse((UnsubscribeRequest) request);
                            break;
                        case "close":
                            resp = new CloseResponse((CloseRequest) request);
                            break;
                        default:
                            throw new RuntimeException("Unknown method");
                    }
                    resp.populate(o.getArray("update"));
                } else {
                    // Subscription update
                    SubscribeRequest req = (SubscribeRequest) request;
                    resp = new SubscriptionResponse(req, getManager());
                    resp.populate(o.getArray("update"));
                }
                if (responseHandler != null) {
                    responseHandler.handle(resp);
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

    public void setResponseHandler(Handler<Response<?>> handler) {
        checkConnected();
        this.responseHandler = handler;
    }
}
