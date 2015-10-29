package org.dsa.iot.broker.processor.stream;

import org.dsa.iot.broker.processor.Responder;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

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
        reqMap.put(requester, requesterRid);
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
                        }
                        cache.put(name, array);
                    }
                } finally {
                    cacheLock.writeLock().unlock();
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
}
