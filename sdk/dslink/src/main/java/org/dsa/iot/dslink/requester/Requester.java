package org.dsa.iot.dslink.requester;

import lombok.NonNull;
import lombok.val;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.requester.requests.*;
import org.dsa.iot.dslink.requester.responses.*;
import org.dsa.iot.dslink.util.Linkable;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class Requester extends Linkable {
    
    public Requester(MBassador<Event> bus) {
        super(bus);
    }

    public void sendRequest(@NonNull Client client,
                            @NonNull Request req) {
        ensureConnected();

        val obj = new JsonObject();
        {
            val rid = client.getRequestTracker().track(req);
            obj.putNumber("rid", rid);
            obj.putString("method", req.getName());
            req.addJsonValues(obj);
        }
        
        val top = new JsonObject();
        val requests = new JsonArray();
        requests.add(obj);
        top.putArray("requests", requests);
        client.write(top);
        System.out.println(client.getDsId() + " => " + top.encode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parse(Client client, JsonArray responses) {
        val it = responses.iterator();
        for (JsonObject o; it.hasNext();) {
            o = (JsonObject) it.next();
            handleResponse(client, o);
        }
    }
    
    public void handleResponse(Client client, JsonObject obj) {
        val rid = obj.getNumber("rid").intValue();
        val request = client.getRequestTracker().getRequest(rid);
        Response<?> resp;
        if (rid != 0) {
            // Response
            val state = obj.getString("state");
            if (StreamState.CLOSED.jsonName.equals(state)) {
                client.getRequestTracker().untrack(rid);
            }
            resp = getResponse(request);
        } else {
            // Subscription update
            val req = (SubscribeRequest) request;
            resp = new SubscriptionResponse(req, getManager());
        }
        resp.populate(obj.getArray("updates"));
        synchronized (this) {
            val name = request.getName();
            val ev = new ResponseEvent(client, rid, name, resp);
            getBus().publish(ev);
        }
    }
    
    public Response<?> getResponse(Request req) {
        val man = getManager();
        switch (req.getName()) {
            case "list":
                return new ListResponse((ListRequest) req, man);
            case "set":
                return new SetResponse((SetRequest) req);
            case "remove":
                return new RemoveResponse((RemoveRequest) req);
            case "invoke":
                return new InvokeResponse((InvokeRequest) req);
            case "subscribe":
                return new SubscriptionResponse((SubscribeRequest) req, man);
            case "unsubscribe":
                return new UnsubscribeResponse((UnsubscribeRequest) req);
            case "close":
                return new CloseResponse((CloseRequest) req);
            default:
                throw new RuntimeException("Unknown method");
        }
    }
}
