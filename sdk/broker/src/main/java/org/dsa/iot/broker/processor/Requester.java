package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.methods.ListResponse;
import org.dsa.iot.broker.processor.stream.Stream;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Samuel Grenier
 */
public class Requester extends LinkHandler {

    private final Map<Integer, Stream> reqStreams = new ConcurrentHashMap<>();

    public Requester(DSLinkNode node) {
        super(node);
    }

    public void addStream(int rid, Stream stream) {
        if (stream == null) {
            throw new NullPointerException("stream");
        }
        reqStreams.put(rid, stream);
    }

    public Stream removeStream(int rid) {
        return reqStreams.remove(rid);
    }

    protected Map<Integer, Stream> getReqStreams() {
        return Collections.unmodifiableMap(reqStreams);
    }

    protected void processRequest(JsonObject request) {
        final Broker broker = client().broker();
        final String method = request.get("method");
        final int rid = request.get("rid");
        JsonObject resp = null;
        switch (method) {
            case "list": {
                String path = request.get("path");
                ListResponse list = new ListResponse(broker, path);
                resp = list.getResponse(client(), rid);
                break;
            }
            case "set": {
                break;
            }
            case "remove": {
                break;
            }
            case "close": {
                Stream stream = removeStream(rid);
                if (stream != null) {
                    stream.close(this);
                }
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

            client().write(top.encode());
        }
    }
}
