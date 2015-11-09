package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.node.BrokerNode;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.stream.InvokeStream;
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
                ParsedPath pp = parse(request.get("path"));
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
                    stream.close(client(), true);
                }
                break;
            }
            case "subscribe": {
                JsonArray paths = request.get("paths");
                for (Object object : paths) {
                    JsonObject obj = (JsonObject) object;
                    ParsedPath path = parse(obj.get("path"));
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
                            ParsedPath pp = stream.path();
                            subPathSids.remove(pp);
                            BrokerNode<?> node = broker.getTree().getNode(pp);
                            node.unsubscribe(stream, client());
                        }
                    }
                }
                resp = closed();
                break;
            }
            case "invoke": {
                ParsedPath path = parse(request.get("path"));
                JsonObject params = request.get("params");
                BrokerNode<?> node = broker.getTree().getNode(path);
                InvokeStream stream = node.invoke(path, client(), rid, params, null);
                addStream(rid, stream);
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

    private ParsedPath parse(Object obj) {
        return parse((String) obj);
    }

    private ParsedPath parse(String path) {
        Objects.requireNonNull(path, "path");
        return ParsedPath.parse(client().broker().downstream(), path);
    }
}
