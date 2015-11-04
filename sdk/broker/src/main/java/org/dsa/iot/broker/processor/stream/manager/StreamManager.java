package org.dsa.iot.broker.processor.stream.manager;

import io.netty.util.internal.ConcurrentSet;
import org.dsa.iot.broker.processor.Responder;
import org.dsa.iot.broker.processor.stream.ListStream;
import org.dsa.iot.broker.processor.stream.Stream;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Samuel Grenier
 */
public class StreamManager {

    private final ReentrantReadWriteLock streamLock = new ReentrantReadWriteLock();
    private final Map<Integer, Stream> streamMap = new HashMap<>();
    private final Set<Stream> streamSet = new ConcurrentSet<>();

    private final ListStreamManager lsm = new ListStreamManager(this);
    private final WeakReference<Responder> responder;

    public StreamManager(Responder responder) {
        Objects.requireNonNull(responder);
        this.responder = new WeakReference<>(responder);
    }

    public ListStreamManager list() {
        return lsm;
    }

    public void close(Client requester, Stream stream) {
        close(requester, Collections.singleton(stream));
    }

    public void close(Client requester, Collection<Stream> streams) {
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
            streamLock.writeLock().lock();
            Integer respRid = lsm.remove(stream);
            if (respRid != null) {
                streamMap.remove(respRid);
            }
            streamLock.writeLock().unlock();

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
            responder().client().write(top.encode());
        }
    }

    public void responderConnected() {
        for (Stream stream : streamSet) {
            stream.responderConnected();
        }
    }

    public void responderDisconnected() {
        for (Stream stream : streamSet) {
            stream.responderDisconnected();
        }
    }

    public Responder responder() {
        return responder.get();
    }

    public Stream get(Integer rid) {
        if (rid == null) {
            return null;
        }
        streamLock.readLock().lock();
        try {
            return streamMap.get(rid);
        } finally {
            streamLock.readLock().unlock();
        }
    }

    public Stream addIfNull(Integer rid, Stream stream) {
        Objects.requireNonNull(rid, "rid");
        Objects.requireNonNull(stream, "stream");
        streamLock.writeLock().lock();
        try {
            Stream orig = streamMap.get(rid);
            if (orig == null) {
                orig = stream;
                streamMap.put(rid, stream);
                streamSet.add(stream);
            }
            return orig;
        } finally {
            streamLock.writeLock().unlock();
        }
    }

    public Stream remove(Integer rid) {
        if (rid == null) {
            return null;
        }
        Stream stream;
        streamLock.writeLock().lock();
        try {
            stream = streamMap.remove(rid);
            if (stream != null) {
                streamSet.remove(stream);
            }
        } finally {
            streamLock.writeLock().unlock();
        }
        if (stream instanceof ListStream) {
            lsm.remove(stream);
        } else if (stream != null) {
            String name = stream.getClass().getName();
            throw new IllegalStateException("Unhandled class: " + name);
        }
        return stream;
    }
}
