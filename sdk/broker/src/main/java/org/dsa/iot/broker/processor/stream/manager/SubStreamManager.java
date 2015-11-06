package org.dsa.iot.broker.processor.stream.manager;

import org.dsa.iot.broker.processor.Responder;
import org.dsa.iot.broker.processor.stream.SubStream;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Samuel Grenier
 */
public class SubStreamManager {

    private final ReentrantReadWriteLock subLock = new ReentrantReadWriteLock();
    private final Map<Integer, SubStream> subStreams = new HashMap<>();
    private final Map<ParsedPath, Integer> subPaths = new HashMap<>();
    private final WeakReference<StreamManager> manager;

    public SubStreamManager(StreamManager manager) {
        this.manager = new WeakReference<>(manager);
    }

    public SubStream subscribe(ParsedPath path, Client requester, int sid) {
        Integer responderSid;
        SubStream stream = null;

        subLock.readLock().lock();
        try {
            responderSid = subPaths.get(path);
            if (responderSid != null) {
                stream = subStreams.get(responderSid);
            }
        } finally {
            subLock.readLock().unlock();
        }

        JsonObject top = null;
        if (responderSid == null) {
            subLock.writeLock().lock();
            try {
                responderSid = subPaths.get(path);
                if (responderSid != null) {
                    stream = subStreams.get(responderSid);
                } else {
                    JsonObject req = new JsonObject();
                    req.put("rid", responder().nextRid());
                    req.put("method", "subscribe");
                    {
                        JsonArray paths = new JsonArray();
                        {
                            responderSid = responder().nextSid();
                            subPaths.put(path, responderSid);
                            JsonObject obj = new JsonObject();
                            obj.put("path", path.base());
                            obj.put("sid", responderSid);
                            paths.add(obj);
                        }
                        req.put("paths", paths);
                    }

                    JsonArray reqs = new JsonArray();
                    reqs.add(req);
                    top = new JsonObject();
                    top.put("requests", reqs);

                    stream = new SubStream(responder(), path);
                    subStreams.put(responderSid, stream);
                }
            } finally {
                subLock.writeLock().unlock();
            }
        }

        stream.add(requester, sid);
        if (top != null) {
            responder().client().write(top.encode());
        }
        return stream;
    }

    public void unsubscribe(SubStream stream, Client requester) {
        Integer sid;
        subLock.readLock().lock();
        try {
            sid = subPaths.get(stream.path());
        } finally {
            subLock.readLock().unlock();
        }
        if (sid == null) {
            return;
        }
        stream.remove(requester);
        if (stream.isEmpty()) {
            subLock.writeLock().lock();
            try {
                subPaths.remove(stream.path());
                subStreams.remove(sid);
            } finally {
                subLock.writeLock().unlock();
            }

            JsonObject req = new JsonObject();
            req.put("rid", responder().nextRid());
            req.put("method", "unsubscribe");
            JsonArray sids = new JsonArray();
            sids.add(sid);
            req.put("sids", sids);

            JsonArray reqs = new JsonArray();
            reqs.add(req);

            JsonObject top = new JsonObject();
            top.put("requests", reqs);
            responder().client().write(top.encode());
        }
    }

    public void dispatch(JsonArray updates) {
        if (updates == null) {
            return;
        }
        subLock.readLock().lock();
        try {
            for (Object obj : updates) {
                JsonArray update = (JsonArray) obj;
                Integer sid = update.get(0);
                SubStream stream = subStreams.get(sid);
                if (stream != null) {
                    stream.dispatch(update);
                }
            }
        } finally {
            subLock.readLock().unlock();
        }
    }

    public Responder responder() {
        StreamManager manager = this.manager.get();
        return manager != null ? manager.responder() : null;
    }
}
