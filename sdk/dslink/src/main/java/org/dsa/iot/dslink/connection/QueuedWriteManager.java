package org.dsa.iot.dslink.connection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.internal.SystemPropertyUtil;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.PropertyReference;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class QueuedWriteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedWriteManager.class);
    private static final int DISPATCH_DELAY;

    private final Map<Integer, JsonObject> mergedTasks = new HashMap<>();
    private final List<JsonObject> rawTasks = new LinkedList<>();
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

    @SuppressFBWarnings("SWL_SLEEP_WITH_LOCK_HELD")
    public boolean post(JsonObject content, boolean merge) {
        synchronized (this) {
            while (shouldBlock()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (shouldQueue()) {
                addTask(content, merge);
                schedule();
                return false;
            }
        }

        JsonArray updates = new JsonArray();
        updates.add(content);

        JsonObject top = new JsonObject();
        top.put(topName, updates);
        forceWrite(top);
        return true;
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
                            if (update instanceof JsonArray
                                    || update instanceof JsonObject) {
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
                        if (rawTasks.isEmpty() && mergedTasks.isEmpty()) {
                            return;
                        }
                        JsonArray updates = new JsonArray();
                        Iterator<JsonObject> it = mergedTasks.values().iterator();
                        while (it.hasNext()) {
                            updates.add(it.next());
                            it.remove();
                        }
                        it = rawTasks.iterator();
                        while (it.hasNext()) {
                            updates.add(it.next());
                            it.remove();
                        }

                        JsonObject top = new JsonObject();
                        top.put(topName, updates);
                        forceWrite(top);
                    }
                    if (schedule) {
                        schedule();
                    }
                }
            }
        }, DISPATCH_DELAY, TimeUnit.MILLISECONDS);
    }

    private synchronized boolean shouldBlock() {
        return (mergedTasks.size() + rawTasks.size()) > 100000;
    }

    private synchronized boolean shouldQueue() {
        return !client.writable()
                || tracker.missingAckCount() > 8 || fut != null;
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
