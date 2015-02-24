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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Samuel Grenier
 */
public class Requester extends Linkable {

    private final AtomicInteger gid;
    private final Map<Client, Map<Integer, Integer>> gidMap;
    
    public Requester(MBassador<Event> bus) {
        super(bus);
        gid = new AtomicInteger();
        gidMap = new HashMap<>(); // TODO: test with weak hash map
    }

    public int sendRequest(@NonNull Client client,
                            @NonNull Request req) {
        ensureConnected();

        val obj = new JsonObject();
        val rid = client.getRequestTracker().track(req);
        obj.putNumber("rid", rid);
        obj.putString("method", req.getName());
        req.addJsonValues(obj);

        val requests = new JsonArray();
        requests.add(obj);
        
        int id = gid.getAndIncrement();
        synchronized (this) {
            Map<Integer, Integer> map = gidMap.get(client);
            if (map == null) {
                map = new HashMap<>();
                gidMap.put(client, map);
            }
            map.put(rid, id);
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
            int gid = -1;
            if (rid != 0) {
                // Response
                String state = o.getString("state");
                synchronized (this) {
                    val map = gidMap.get(client);
                    if (StreamState.CLOSED.jsonName.equals(state)) {
                        client.getRequestTracker().untrack(rid);
                        gid = map.remove(rid);
                    } else {
                        gid = map.get(rid);
                    }
                }

                resp = getResponse(gid, request);
                resp.populate(o.getArray("updates"));
            } else {
                // Subscription update
                SubscribeRequest req = (SubscribeRequest) request;
                resp = new SubscriptionResponse(req, getManager());
                resp.populate(o.getArray("updates"));
            }
            synchronized (this) {
                val ev = new ResponseEvent(client, gid, rid, name, resp);
                getBus().publish(ev);
            }
        }
    }
    
    public Response<?> getResponse(int gid, Request req) {
        switch (req.getName()) {
            case "list":
                return new ListResponse((ListRequest) req, getManager());
            case "set":
                return new SetResponse((SetRequest) req);
            case "remove":
                return new RemoveResponse((RemoveRequest) req);
            case "invoke":
                return new InvokeResponse((InvokeRequest) req);
            case "subscribe":
                return new SubscribeResponse((SubscribeRequest) req);
            case "unsubscribe":
                return new UnsubscribeResponse((UnsubscribeRequest) req);
            case "close":
                return new CloseResponse((CloseRequest) req);
            default:
                throw new RuntimeException("Unknown method");
        }
    }
}
