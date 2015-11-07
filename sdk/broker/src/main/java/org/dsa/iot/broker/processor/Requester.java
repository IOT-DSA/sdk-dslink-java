package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.node.BrokerNode;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.stream.Stream;
import org.dsa.iot.broker.processor.stream.SubStream;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Samuel Grenier
 */
public class Requester extends LinkHandler {

    private final Map<Integer, Stream> reqStreams = new ConcurrentHashMap<>();
    private final Map<Integer, SubStream> subStreams = new HashMap<>();
    private final Map<ParsedPath, Integer> subPathSids = new HashMap<>();

    public Requester(DSLinkNode node) {
        super(node);
    }

    public void addStream(int rid, Stream stream) {
        reqStreams.put(rid, Objects.requireNonNull(stream, "stream"));
    }

    public Stream removeStream(int rid) {
        return reqStreams.remove(rid);
    }

    protected Map<Integer, Stream> getReqStreams() {
        return Collections.unmodifiableMap(reqStreams);
    }

    @Override
    protected void process(JsonObject request) {
        final Broker broker = client().broker();
        final String method = request.get("method");
        final int rid = request.get("rid");
        JsonObject resp = null;
        switch (method) {
            case "list": {
                String path = request.get("path");
                ParsedPath pp = ParsedPath.parse(broker.downstream(), path);
                BrokerNode<?> node = broker.getTree().getNode(pp);
                resp = node != null ? node.list(pp, client(), rid) : null;
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
                JsonArray paths = request.get("paths");
                for (Object object : paths) {
                    JsonObject obj = (JsonObject) object;
                    String p = obj.get("path");
                    ParsedPath path = ParsedPath.parse(broker.downstream(), p);
                    Integer sid = obj.get("sid");

                    BrokerNode node = broker.getTree().getNode(path);
                    SubStream stream = node.subscribe(path, client(), sid);

                    synchronized (subPathSids) {
                        Integer prev = subPathSids.put(path, sid);
                        if (prev != null) {
                            subStreams.remove(prev);
                        }
                        subStreams.put(sid, stream);
                    }
                }
                resp = closed();
                break;
            }
            case "unsubscribe": {
                JsonArray sids = request.get("sids");
                for (Object object : sids) {
                    Integer sid = (Integer) object;

                    synchronized (subPathSids) {
                        SubStream stream = subStreams.remove(sid);
                        if (stream != null) {
                            subPathSids.remove(stream.path());
                            Responder r = stream.responder();
                            r.stream().sub().unsubscribe(stream, client());
                        }
                    }
                }
                resp = closed();
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

    private JsonObject closed() {
        JsonObject resp = new JsonObject();
        resp.put("stream", StreamState.CLOSED.getJsonName());
        return resp;
    }
}
