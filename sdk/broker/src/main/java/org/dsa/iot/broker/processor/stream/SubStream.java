package org.dsa.iot.broker.processor.stream;

import org.dsa.iot.broker.processor.Responder;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Samuel Grenier
 */
public class SubStream {

    private final Responder responder;
    private final ParsedPath path;

    private Map<Client, Integer> clientMap = new ConcurrentHashMap<>();
    private JsonArray lastValueUpdate;

    public SubStream(Responder responder, ParsedPath path) {
        this.responder = Objects.requireNonNull(responder, "responder");
        this.path = Objects.requireNonNull(path, "path");
    }

    public Responder responder() {
        return responder;
    }

    public ParsedPath path() {
        return path;
    }

    public void add(Client requester, int sid) {
        Integer prev = clientMap.put(requester, sid);
        if (prev != null) {
            return;
        }
        JsonArray lastValueUpdate = this.lastValueUpdate;
        if (lastValueUpdate != null) {
            JsonObject resp = new JsonObject();
            resp.put("rid", 0);

            JsonArray update = new JsonArray();
            update.add(sid);
            update.add(lastValueUpdate.get(1));
            update.add(lastValueUpdate.get(2));

            JsonArray updates = new JsonArray();
            updates.add(update);
            resp.put("updates", updates);

            JsonArray resps = new JsonArray();
            resps.add(resp);

            JsonObject top = new JsonObject();
            top.put("responses", resps);

            requester.write(top.encode());
        }
    }

    public void remove(Client requester) {
        clientMap.remove(requester);
    }

    public boolean isEmpty() {
        return clientMap.isEmpty();
    }

    public void dispatch(JsonArray update) {
        lastValueUpdate = update;

        JsonObject resp = new JsonObject();
        resp.put("rid", 0);

        JsonArray updates = new JsonArray();
        updates.add(update);
        resp.put("updates", updates);

        JsonArray resps = new JsonArray();
        resps.add(resp);

        JsonObject top = new JsonObject();
        top.put("responses", resps);

        for (Map.Entry<Client, Integer> entry : clientMap.entrySet()) {
            Integer sid = entry.getValue();
            update.set(0, sid);

            Client requester = entry.getKey();
            requester.write(top.encode());
        }
    }
}
