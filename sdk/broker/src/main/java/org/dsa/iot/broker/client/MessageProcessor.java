package org.dsa.iot.broker.client;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.methods.ListResponse;
import org.dsa.iot.broker.utils.Dispatch;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Samuel Grenier
 */
public class MessageProcessor {

    private final Map<Integer, Dispatch> dispatchMap = new ConcurrentHashMap<>();
    private final Client client;

    public MessageProcessor(Client client) {
        this.client = client;
    }

    public void addDispatch(int rid, Dispatch dispatch) {
        dispatchMap.put(rid, dispatch);
    }

    public void processData(JsonObject data) {
        if (client.handshake().isRequester()) {
            processRequests((JsonArray) data.get("requests"));
        }
        if (client.handshake().isResponder()) {
            processResponses((JsonArray) data.get("responses"));
        }
    }

    protected void processRequests(JsonArray requests) {
        if (requests != null) {
            for (Object obj : requests) {
                processRequest((JsonObject) obj);
            }
        }
    }

    protected void processResponses(JsonArray responses) {
        if (responses != null) {
            for (Object obj : responses) {
                processResponse((JsonObject) obj);
            }
        }
    }

    protected void processRequest(JsonObject request) {
        final Broker broker = client.broker();
        final String method = request.get("method");
        final int rid = request.get("rid");
        JsonObject resp = null;
        switch (method) {
            case "list": {
                String path = request.get("path");
                ListResponse list = new ListResponse(broker, path);
                resp = list.getResponse(client, rid);
                break;
            }
            case "set": {
                break;
            }
            case "remove": {
                break;
            }
            case "close": {
                break;
            }
            case "subscribe": {
                break;
            }
            case "unsubscribe": {
                break;
            }
            case "invoke": {
                break;
            }
            default:
                throw new RuntimeException("Unsupported method: " + method);
        }
        if (resp != null) {
            resp.put("rid", rid);

            JsonArray resps = new JsonArray();
            resps.add(resp);

            JsonObject top = new JsonObject();
            top.put("responses", resps);

            client.write(top.encode());
        }
    }

    protected void processResponse(JsonObject response) {
        Integer rid = response.get("rid");
        Dispatch dispatch = dispatchMap.get(rid);
        if (dispatch != null) {
            response.put("rid", dispatch.getRid());

            JsonArray resps = new JsonArray();
            resps.add(response);

            JsonObject top = new JsonObject();
            top.put("responses", resps);

            Client client = dispatch.getClient();
            client.write(top.encode());
        }
    }
}
