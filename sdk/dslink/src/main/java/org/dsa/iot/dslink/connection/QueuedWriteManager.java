package org.dsa.iot.dslink.connection;

import io.netty.util.internal.SystemPropertyUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.PropertyReference;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueuedWriteManager implements Runnable {

    private static final int DISPATCH_DELAY;
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedWriteManager.class);
    private static final int MAX_TASKS = 1000;
    private final NetworkClient client;
    private final EncodingFormat format;
    private ScheduledFuture<?> fut;
    private final Map<Integer, JsonObject> mergedTasks = new HashMap<>();
    private final List<JsonObject> rawTasks = new LinkedList<>();
    private final String topName;
    private final MessageTracker tracker;
    private final Object writeMutex = new Object();

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

    public boolean post(JsonObject content, boolean merge) {
        synchronized (this) {
            if (shouldQueue()) {
                addTask(content, merge);
                schedule(DISPATCH_DELAY);
                return false;
            }
        }
        JsonArray updates = new JsonArray();
        updates.add(content);
        forceWriteUpdates(updates);
        return true;
    }

    public void run() {
        final JsonArray updates;
        synchronized (this) {
            fut = null;
            if (shouldQueue()) {
                updates = null;
            } else {
                updates = fetchUpdates();
            }
        }
        if (updates != null) {
            forceWriteUpdates(updates);
        }
        synchronized (this) {
            if (hasTasks()) {
                schedule(1);
            }
        }
    }

    private synchronized void addTask(JsonObject content, boolean merge) {
        if (merge) {
            int rid = content.get("rid");
            JsonObject obj = mergedTasks.get(rid);
            if (obj == null) {
                mergedTasks.put(rid, content);
            } else {
                JsonArray oldUpdates = obj.get("updates");
                if (oldUpdates != null) {
                    JsonArray newUpdates = content.remove("updates");
                    if (newUpdates != null) {
                        for (Object update : newUpdates) {
                            if (update instanceof JsonArray || update instanceof JsonObject) {
                                oldUpdates.add(update);
                            } else {
                                String clazz = update.getClass().getName();
                                String err = "Unhandled type: " + clazz;
                                throw new RuntimeException(err);
                            }
                        }
                    }
                    obj.mergeIn(content);
                } else {
                    obj.mergeIn(content);
                }
            }
        } else {
            rawTasks.add(content);
        }
    }

    private JsonArray fetchUpdates() {
        if (!hasTasks()) {
            return null;
        }
        JsonArray updates = new JsonArray();
        Iterator<JsonObject> it = mergedTasks.values().iterator();
        int count = MAX_TASKS / 2;
        while (it.hasNext() && (--count >= 0)) {
            updates.add(it.next());
            it.remove();
        }
        it = rawTasks.iterator();
        count += (MAX_TASKS / 2);
        while (it.hasNext() && (--count >= 0)) {
            updates.add(it.next());
            it.remove();
        }
        return updates;
    }

    private void forceWrite(JsonObject obj) {
        synchronized (writeMutex) {
            obj.put("msg", tracker.incrementMessageId());
            client.write(format, obj);
        }
    }

    private void forceWriteUpdates(JsonArray updates) {
        JsonObject top = new JsonObject();
        top.put(topName, updates);
        forceWrite(top);
    }

    private boolean hasTasks() {
        return !(rawTasks.isEmpty() && mergedTasks.isEmpty());
    }

    private void schedule(long millis) {
        if (fut != null) {
            return;
        }
        fut = LoopProvider.getProvider().schedule(this, millis, TimeUnit.MILLISECONDS);
    }

    private boolean shouldQueue() {
        return (fut != null) || (tracker.missingAckCount() > 8) || !client.writable();
    }

    static {
        String s = PropertyReference.DISPATCH_DELAY;
        DISPATCH_DELAY = SystemPropertyUtil.getInt(s, 10);
        LOGGER.debug("-D{}: {}", s, DISPATCH_DELAY);
    }
}
