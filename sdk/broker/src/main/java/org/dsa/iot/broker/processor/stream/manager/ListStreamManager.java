package org.dsa.iot.broker.processor.stream.manager;

import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.Responder;
import org.dsa.iot.broker.processor.stream.ListStream;
import org.dsa.iot.broker.processor.stream.Stream;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.broker.utils.RequestGenerator;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Samuel Grenier
 */
public class ListStreamManager {

    private final ReentrantReadWriteLock listLock = new ReentrantReadWriteLock();
    private final Map<ParsedPath, Integer> pathListMap = new HashMap<>();
    private final WeakReference<StreamManager> manager;

    public ListStreamManager(StreamManager manager) {
        Objects.requireNonNull(manager, "manager");
        this.manager = new WeakReference<>(manager);
    }

    public void add(ParsedPath path,
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
                    Responder responder = manager().responder();
                    rid = responder.nextRid();
                    Client client = responder.client();
                    if (client != null) {
                        pathListMap.put(path, rid);
                        JsonArray req = RequestGenerator.list(path, rid);
                        client.writeRequest(req);
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
                                update.add(responder.node().disconnected());
                                updates.add(update);
                            }
                            {
                                JsonArray update = responder.node().linkDataUpdate();
                                if (update != null) {
                                    updates.add(update);
                                }
                            }
                            resp.put("updates", updates);
                        }

                        JsonArray resps = new JsonArray();
                        resps.add(resp);
                        requester.writeResponse(resps);
                    }
                }
            } finally {
                listLock.writeLock().unlock();
            }
        }

        Stream stream = manager().get(rid);
        if (stream == null) {
            stream = new ListStream(manager().responder(), path);
            stream = manager().addIfNull(rid, stream);
        }

        stream.add(requester, requesterRid);
        requester.processor().requester().addStream(requesterRid, stream);
    }

    public void move(ListStream stream, int rid) {
        Integer orig;
        listLock.writeLock().lock();
        try {
            orig = pathListMap.put(stream.path(), rid);
        } finally {
            listLock.writeLock().unlock();
        }

        manager().remove(orig);
        manager().addIfNull(rid, stream);
    }

    public Integer remove(Stream stream) {
        if (stream == null) {
            return null;
        }
        listLock.writeLock().lock();
        try {
            ParsedPath path = stream.path();
            if (path != null) {
                return pathListMap.remove(path);
            }
        } finally {
            listLock.writeLock().unlock();
        }
        return null;
    }

    protected StreamManager manager() {
        return manager.get();
    }
}
