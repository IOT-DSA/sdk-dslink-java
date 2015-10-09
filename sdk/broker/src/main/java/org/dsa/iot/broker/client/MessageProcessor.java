package org.dsa.iot.broker.client;

import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class MessageProcessor {

    private final Client client;

    public MessageProcessor(Client client) {
        this.client = client;
    }

    public void processData(JsonObject data) {
        if (client.isRequester()) {
            processRequests((JsonArray) data.get("requests"));
        }
        if (client.isResponder()) {
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
        String method = request.get("method");
        JsonObject resp = null;
        switch (method) {
            case "list": {
                String path = request.get("path");
                resp = client.getBroker().getTree().list(path);
                if (resp != null) {
                    int rid = request.get("rid");
                    resp.put("rid", rid);
                }
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
            JsonArray resps = new JsonArray();
            resps.add(resp);

            JsonObject top = new JsonObject();
            top.put("responses", resps);

            client.write(top.encode());
        }
    }

    protected void processResponse(JsonObject response) {
        throw new UnsupportedOperationException();
    }
}
