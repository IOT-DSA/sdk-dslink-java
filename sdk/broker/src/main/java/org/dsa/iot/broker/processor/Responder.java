package org.dsa.iot.broker.processor;

import io.netty.util.internal.ConcurrentSet;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.methods.ListResponse;
import org.dsa.iot.broker.processor.stream.ListStream;
import org.dsa.iot.broker.processor.stream.Stream;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Samuel Grenier
 */
public class Responder extends LinkHandler {

    private final ReentrantReadWriteLock listLock = new ReentrantReadWriteLock();
    private final Map<ParsedPath, Integer> pathListMap = new HashMap<>();

    private final ReentrantReadWriteLock streamLock = new ReentrantReadWriteLock();
    private final Map<Integer, Stream> streamMap = new HashMap<>();
    private final Set<Stream> streamSet = new ConcurrentSet<>();

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
            rid = pathListMap.get(path);
        } finally {
            listLock.readLock().unlock();
        }

        if (rid == null) {
            listLock.writeLock().lock();
            try {
                rid = pathListMap.get(path);
                if (rid == null) {
                    Client responder = client();
                    if (responder != null) {
                        rid = responder.nextRid();
                        pathListMap.put(path, rid);

                        JsonObject top = ListResponse.generateRequest(path, rid);
                        responder.write(top.encode());
                    } else if (path.base().equals("/")) {
                        JsonObject resp = new JsonObject();
                        resp.put("rid", requesterRid);
                        resp.put("stream", StreamState.OPEN.getJsonName());
                        {
                            JsonArray updates = new JsonArray();
                            {
                                JsonArray update = new JsonArray();
                                update.add("$is");
                                update.add(DSLinkNode.PROFILE);
                                updates.add(update);
                            }
                            {
                                JsonArray update = new JsonArray();
                                update.add("$disconnectedTs");
                                update.add(node().disconnected());
                                updates.add(update);
                            }
                            {
                                JsonArray update = node().linkDataUpdate();
                                if (update != null) {
                                    updates.add(update);
                                }
                            }
                            resp.put("updates", updates);
                        }

                        JsonArray resps = new JsonArray();
                        resps.add(resp);

                        JsonObject top = new JsonObject();
                        top.put("responses", resps);
                        requester.write(top.encode());
                    }
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
                    streamSet.add(stream);
                }
            } finally {
                streamLock.writeLock().unlock();
            }
        }

        stream.add(requester, requesterRid);
        requester.processor().requester().addStream(requesterRid, stream);
    }

    public void closeStream(Client requester, Stream stream) {
        closeStream(requester, Collections.singleton(stream));
    }

    public void closeStream(Client requester, Collection<Stream> streams) {
        if (streams == null) {
            return;
        }

        JsonArray reqs = null;
        for (Stream stream : streams) {
            if (stream == null || !streamSet.contains(stream)) {
                continue;
            }
            stream.remove(requester);
            if (!stream.isEmpty()) {
                continue;
            }

            streamSet.remove(stream);
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

                if (reqs == null) {
                    reqs = new JsonArray();
                }
                reqs.add(req);
            }
        }
        if (reqs != null) {
            JsonObject top = new JsonObject();
            top.put("requests", reqs);
            client().write(top.encode());
        }
    }

    public void requesterDisconnected(Client client) {
        Requester req = client.node().processor().requester();
        Map<Integer, Stream> streams = req.getReqStreams();
        if (streams != null) {
            closeStream(client, streams.values());
        }
    }

    public void responderDisconnected() {
        streamLock.writeLock().lock();
        try {
            for (Stream stream : streamMap.values()) {
                stream.responderDisconnected();
            }
        } finally {
            streamLock.writeLock().unlock();
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
                if (stream != null) {
                    streamSet.remove(stream);
                }
                if (stream instanceof ListStream) {
                    listLock.writeLock().lock();
                    ParsedPath path = stream.path();
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
