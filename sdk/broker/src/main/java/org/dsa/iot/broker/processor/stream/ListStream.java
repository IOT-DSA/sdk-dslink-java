package org.dsa.iot.broker.processor.stream;

import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.Responder;
import org.dsa.iot.broker.processor.methods.ListResponse;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Samuel Grenier
 */
public class ListStream extends Stream {

    private final Map<Client, Integer> reqMap = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Map<String, JsonArray> cache = new ConcurrentHashMap<>();

    public ListStream(Responder responder, ParsedPath path) {
        super(responder, path);
    }

    @Override
    public void add(Client requester, int requesterRid) {
        {
            Integer old = reqMap.put(requester, requesterRid);
            if (old != null) {
                return;
            }
        }

        cacheLock.readLock().lock();
        try {
            if (cache.isEmpty()) {
                return;
            }
            JsonArray updates = new JsonArray();
            for (JsonArray update : cache.values()) {
                updates.add(update);
            }

            JsonObject resp = new JsonObject();
            resp.put("rid", requesterRid);
            resp.put("stream", StreamState.OPEN.getJsonName());
            resp.put("updates", updates);

            JsonArray resps = new JsonArray();
            resps.add(resp);

            JsonObject top = new JsonObject();
            top.put("responses", resps);
            requester.write(top.encode());
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public void remove(Client requester) {
        reqMap.remove(requester);
    }

    @Override
    public boolean isEmpty() {
        return reqMap.isEmpty();
    }

    @Override
    public void dispatch(StreamState state, JsonObject response) {
        JsonArray updates = response.get("updates");
        if (updates != null) {
            List<JsonArray> injectedData = null;
            for (Object obj : updates) {
                cacheLock.writeLock().lock();
                try {
                    if (obj instanceof JsonObject) {
                        JsonObject json = (JsonObject) obj;
                        String name = json.get("name");
                        String change = json.get("change");
                        if ("remove".equals(change)) {
                            cache.remove(name);
                        }
                    } else if (obj instanceof JsonArray) {
                        JsonArray array = (JsonArray) obj;
                        String name = array.get(0);
                        if (name.equals("$is")) {
                            cache.clear();
                            if (path().base().equals("/")) {
                                DSLinkNode node = responder().node();
                                JsonArray update = node.linkDataUpdate();
                                if (update != null) {
                                    String confName = update.get(0);
                                    cache.put(confName, update);
                                    if (injectedData == null) {
                                        injectedData = new ArrayList<>();
                                    }
                                    injectedData.add(update);
                                }
                            }
                        }
                        cache.put(name, array);
                    }
                } finally {
                    cacheLock.writeLock().unlock();
                }
            }
            if (injectedData != null) {
                for (JsonArray update : injectedData) {
                    updates.add(update);
                }
            }
        }

        JsonArray resps = new JsonArray();
        resps.add(response);
        JsonObject top = new JsonObject();
        top.put("responses", resps);

        for (Map.Entry<Client, Integer> entry : reqMap.entrySet()) {
            Client client = entry.getKey();
            int rid = entry.getValue();
            if (state == StreamState.CLOSED) {
                client.processor().requester().removeStream(rid);
            }

            response.put("rid", rid);
            client.write(top.encode());
        }
    }

    @Override
    public void responderConnected() {
        Client client = responder().client();
        JsonObject top = ListResponse.generateRequest(path(), client.nextRid());
        client.write(top.encode());
    }

    @Override
    public void responderDisconnected() {
        cacheLock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }

        JsonObject response = new JsonObject();
        {
            JsonArray updates = new JsonArray();
            {
                JsonArray update = new JsonArray();
                update.add("$disconnectedTs");
                update.add(responder().node().disconnected());
                updates.add(update);
            }
            response.put("updates", updates);
        }

        JsonArray resps = new JsonArray();
        resps.add(response);
        JsonObject top = new JsonObject();
        top.put("responses", resps);
        for (Map.Entry<Client, Integer> entry : reqMap.entrySet()) {
            Client client = entry.getKey();
            int rid = entry.getValue();
            response.put("rid", rid);
            client.write(top.encode());
        }
    }
}
