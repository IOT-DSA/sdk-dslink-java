package org.dsa.iot.dslink.connection;

import io.netty.util.internal.SystemPropertyUtil;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.PropertyReference;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class QueuedWriteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedWriteManager.class);
    private static final int DISPATCH_DELAY;

    private final Map<Integer, JsonObject> tasks = new HashMap<>();
    private final EncodingFormat format;
    private final MessageTracker tracker;
    private final NetworkClient client;
    private final String topName;
    private ScheduledFuture<?> fut;

    public QueuedWriteManager(NetworkClient client,
                              MessageTracker tracker,
                              EncodingFormat format,
                              String topName) {
        if (client == null) {
            throw new NullPointerException("client");
        } else if (tracker == null) {
            throw new NullPointerException("tracker");
        } else if (format == null) {
            throw new NullPointerException("format");
        } else if (topName == null) {
            throw new NullPointerException("topName");
        }
        this.format = format;
        this.tracker = tracker;
        this.topName = topName;
        this.client = client;
    }

    public synchronized void post(Collection<JsonObject> content) {
        for (JsonObject obj : content) {
            post(obj);
        }
    }

    public synchronized boolean post(JsonObject content) {
        if (shouldQueue()) {
            addTask(content);
            schedule();
            return false;
        }

        JsonArray updates = new JsonArray();
        updates.add(content);

        JsonObject top = new JsonObject();
        top.put(topName, updates);
        forceWrite(top);
        return true;
    }

    private synchronized void addTask(JsonObject content) {
        int rid = content.get("rid");
        JsonObject obj = tasks.get(rid);
        if (obj == null) {
            tasks.put(rid, content);
        } else {
            JsonArray oldUpdates = obj.get("updates");
            if (oldUpdates != null) {
                JsonArray newUpdates = content.remove("updates");
                if (newUpdates != null) {
                    for (Object update : newUpdates) {
                        if (update instanceof JsonArray
                                || update instanceof JsonObject) {
                            oldUpdates.add(update);
                        } else {
                            String clazz = update.getClass().getName();
                            throw new RuntimeException("Unhandled type: " + clazz);
                        }
                    }
                }
                obj.mergeIn(content);
            } else {
                obj.mergeIn(content);
            }
        }
    }

    private synchronized void schedule() {
        if (fut != null) {
            return;
        }
        fut = Objects.getDaemonThreadPool().schedule(new Runnable() {
            @Override
            public void run() {
                boolean schedule = false;
                synchronized (QueuedWriteManager.this) {
                    fut = null;
                    if (shouldQueue()) {
                        schedule = true;
                    } else {
                        if (tasks.isEmpty()) {
                            return;
                        }
                        JsonArray updates = new JsonArray();
                        Iterator<JsonObject> it = tasks.values().iterator();
                        while (it.hasNext()) {
                            updates.add(it.next());
                            it.remove();
                        }
                        JsonObject top = new JsonObject();
                        top.put(topName, updates);
                        forceWrite(top);
                    }
                }
                if (schedule) {
                    schedule();
                }
            }
        }, DISPATCH_DELAY, TimeUnit.MILLISECONDS);
    }

    private boolean shouldQueue() {
        return !client.writable() || tracker.missingAckCount() > 8;
    }

    private synchronized void forceWrite(JsonObject obj) {
        obj.put("msg", tracker.incrementMessageId());
        client.write(format, obj);
    }

    static {
        String s = PropertyReference.DISPATCH_DELAY;
        DISPATCH_DELAY = SystemPropertyUtil.getInt(s, 75);
        LOGGER.debug("-D{}: {}", s, DISPATCH_DELAY);
    }
}
