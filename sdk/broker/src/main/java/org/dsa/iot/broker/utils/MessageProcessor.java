package org.dsa.iot.broker.utils;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.methods.ListResponse;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.stream.ListStream;
import org.dsa.iot.broker.stream.Stream;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Samuel Grenier
 */
public class MessageProcessor {

    private final DSLinkNode node;

    //// Responder data

    private final ReentrantReadWriteLock listLock = new ReentrantReadWriteLock();
    private final Map<String, Integer> pathListMap = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock streamLock = new ReentrantReadWriteLock();
    private final Map<Integer, Stream> streamMap = new ConcurrentHashMap<>();

    //// Requester data

    private final Map<Integer, Stream> reqStreams = new ConcurrentHashMap<>();

    ////

    public MessageProcessor(DSLinkNode node) {
        this.node = node;
    }

    public void addListStream(ParsedPath path,
                              Client requester,
                              int requesterRid) {
        if (path == null) {
            throw new NullPointerException("path");
        } else if (requester == null) {
            throw new NullPointerException("requester");
        } else if (requesterRid <= 0) {
            throw new IllegalArgumentException("requesterRid <= 0");
        }

        Integer rid;
        listLock.readLock().lock();
        try {
            rid = pathListMap.get(path.full());
        } finally {
            listLock.readLock().unlock();
        }

        if (rid == null) {
            listLock.writeLock().lock();
            try {
                rid = pathListMap.get(path.full());
                if (rid == null) {
                    rid = client().nextRid();
                    pathListMap.put(path.full(), rid);

                    JsonObject resp = new JsonObject();
                    resp.put("method", "list");
                    resp.put("rid", rid);
                    resp.put("path", ListResponse.getBasePath(path.split()));

                    JsonArray reqs = new JsonArray();
                    reqs.add(resp);
                    JsonObject top = new JsonObject();
                    top.put("requests", reqs);

                    client().write(top.encode());
                }
            } finally {
                listLock.writeLock().unlock();
            }
        }

        Stream stream;
        streamLock.readLock().lock();
        try {
            stream = streamMap.get(rid);
        } finally {
            streamLock.readLock().unlock();
        }
        if (stream == null) {
            streamLock.writeLock().lock();
            try {
                stream = streamMap.get(rid);
                if (stream == null) {
                    stream = new ListStream(client(), path);
                    streamMap.put(rid, stream);
                }
            } finally {
                streamLock.writeLock().unlock();
            }
        }
        stream.add(requester, requesterRid);
        requester.processor().reqStreams.put(requesterRid, stream);
    }

    public void processData(JsonObject data) {
        if (client().handshake().isRequester()) {
            processRequests((JsonArray) data.get("requests"));
        }
        if (client().handshake().isResponder()) {
            processResponses((JsonArray) data.get("responses"));
        }
    }

    public void removeRequesterStream(int rid) {
        reqStreams.remove(rid);
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
                Stream stream = reqStreams.remove(rid);
                if (stream == null) {
                    break;
                }
                stream.remove(client());
                if (!stream.isEmpty()) {
                    break;
                }

                MessageProcessor proc = stream.responder().processor();
                proc.listLock.writeLock().lock();
                proc.streamLock.writeLock().lock();

                Integer respRid = proc.pathListMap.remove(stream.path());
                if (respRid != null) {
                    proc.streamMap.remove(respRid);
                }

                proc.streamLock.writeLock().unlock();
                proc.listLock.writeLock().unlock();

                if (respRid != null) {
                    JsonObject req = new JsonObject();
                    req.put("rid", respRid);
                    req.put("method", "close");
                    JsonArray reqs = new JsonArray();
                    reqs.add(req);
                    JsonObject top = new JsonObject();
                    top.put("requests", reqs);
                    stream.responder().write(top.encode());
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

    protected void processResponse(JsonObject response) {
        Integer rid = response.get("rid");
        StreamState state = StreamState.toEnum((String) response.get("stream"));
        Stream stream;
        if (state == StreamState.CLOSED) {
            streamLock.writeLock().lock();
            try {
                stream = streamMap.remove(rid);
                if (stream instanceof ListStream) {
                    listLock.writeLock().lock();
                    String path = stream.path();
                    if (path != null) {
                        pathListMap.remove(stream.path());
                    }
                    listLock.writeLock().unlock();
                } else if (stream != null) {
                    String name = stream.getClass().getName();
                    throw new IllegalStateException("Unhandled class: " + name);
                }
            } finally {
                streamLock.writeLock().unlock();
            }
        } else {
            streamLock.readLock().lock();
            stream = streamMap.get(rid);
            streamLock.readLock().unlock();
        }
        if (stream != null) {
            stream.dispatch(state, response);
        }
    }

    protected Client client() {
        return node.client();
    }
}
