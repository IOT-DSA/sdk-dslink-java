package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.methods.ListResponse;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.stream.ListStream;
import org.dsa.iot.broker.stream.Stream;
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
public class Responder extends LinkHandler {

    private final ReentrantReadWriteLock listLock = new ReentrantReadWriteLock();
    private final Map<String, Integer> pathListMap = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock streamLock = new ReentrantReadWriteLock();
    private final Map<Integer, Stream> streamMap = new ConcurrentHashMap<>();

    public Responder(DSLinkNode node) {
        super(node);
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
                    stream = new ListStream(this, path);
                    streamMap.put(rid, stream);
                }
            } finally {
                streamLock.writeLock().unlock();
            }
        }
        stream.add(requester, requesterRid);
        requester.processor().requester().addStream(requesterRid, stream);
    }

    public void closeStream(Client requester, Stream stream) {
        if (stream == null) {
            return;
        }

        stream.remove(requester);
        if (!stream.isEmpty()) {
            return;
        }

        listLock.writeLock().lock();
        streamLock.writeLock().lock();
        Integer respRid = pathListMap.remove(stream.path());
        if (respRid != null) {
            streamMap.remove(respRid);
        }
        streamLock.writeLock().unlock();
        listLock.writeLock().unlock();

        if (respRid != null) {
            JsonObject req = new JsonObject();
            req.put("rid", respRid);
            req.put("method", "close");
            JsonArray reqs = new JsonArray();
            reqs.add(req);
            JsonObject top = new JsonObject();
            top.put("requests", reqs);
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
            try {
                stream = streamMap.get(rid);
            } finally {
                streamLock.readLock().unlock();
            }
        }
        if (stream != null) {
            stream.dispatch(state, response);
        }
    }
}
