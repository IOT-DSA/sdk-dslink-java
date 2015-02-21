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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private final AtomicInteger gid;
    private final Map<Client, Integer> gidMap;
    
    public Requester(MBassador<Event> bus) {
        super(bus);
        gid = new AtomicInteger();
        gidMap = new WeakHashMap<>();
    }

    public int sendRequest(@NonNull Client client,
                            @NonNull Request req) {
        ensureConnected();

        val obj = new JsonObject();
        obj.putNumber("rid", client.getRequestTracker().track(req));
        obj.putString("method", req.getName());
        req.addJsonValues(obj);

        val requests = new JsonArray();
        requests.add(obj);
        
        int id = gid.getAndIncrement();
        synchronized (this) {
            gidMap.put(client, id);
        }

        val top = new JsonObject();
        top.putArray("requests", requests);
        client.write(top);
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parse(Client client, JsonArray responses) {
        val it = responses.iterator();
        for (JsonObject o; it.hasNext();) {
            o = (JsonObject) it.next();

            int rid = o.getNumber("rid").intValue();
            Request request = client.getRequestTracker().getRequest(rid);
            String name = request.getName();
            Response<?> resp;
            if (rid != 0) {
                // Response
                String state = o.getString("state");
                if (StreamState.CLOSED.jsonName.equals(state)) {
                    client.getRequestTracker().untrack(rid);
                }

                switch (name) {
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
                resp.populate(o.getArray("updates"));
            } else {
                // Subscription update
                SubscribeRequest req = (SubscribeRequest) request;
                resp = new SubscriptionResponse(req, getManager());
                resp.populate(o.getArray("updates"));
            }
            synchronized (this) {
                val gid = gidMap.get(client);
                val ev = new ResponseEvent(client, gid, rid, name, resp);
                getBus().publish(ev);
            }
        }
    }
}
