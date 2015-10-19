package org.dsa.iot.broker.stream;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Samuel Grenier
 */
public class ListStream extends Stream {

    // TODO: result caching

    private final Map<Client, Integer> reqMap = new ConcurrentHashMap<>();

    public ListStream(Client responder, ParsedPath path) {
        super(responder, path);
    }

    @Override
    public void add(Client requester, int requesterRid) {
        reqMap.put(requester, requesterRid);
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
        JsonArray resps = new JsonArray();
        resps.add(response);
        JsonObject top = new JsonObject();
        top.put("responses", resps);

        for (Map.Entry<Client, Integer> entry : reqMap.entrySet()) {
            Client client = entry.getKey();
            int rid = entry.getValue();
            if (state == StreamState.CLOSED) {
                client.processor().removeRequesterStream(rid);
            }

            response.put("rid", rid);
            client.write(top.encode());
        }
    }
}
