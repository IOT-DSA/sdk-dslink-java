package org.dsa.iot.dslink.connection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedWriteManager.class);
    private static final int DISPATCH_DELAY;
    //private static final int MAX_RID_BACKLOG = 1000;
    //private static final int MAX_SID_BACKLOG = 50000;
    private static final int RID_CHUNK = 1000;
    private static final int SID_CHUNK = 1000;

    private final Map<Integer, JsonObject> mergedTasks = new HashMap<>();
    private final List<JsonObject> rawTasks = new LinkedList<>();
    private final EncodingFormat format;
    private final MessageTracker tracker;
    private final NetworkClient client;
    private final String topName;
    private boolean open = true;
    private ScheduledFuture<?> fut;
    private final Object writeMutex = new Object();

    //TODO Debug Logging
    private int maxMergedSize = 0;
    private int maxMergedSizeAdder = 1;
    private int maxRawSize = 0;
    private int maxRawSizeAdder = 1;
    private int maxSidUpdates = 0;
    private int maxSidUpdatesAdder = 1;
    private long maxQueueDuration = 0;
    private long maxQueueDurationAdder = 1000;
    private long queueStarted = -1;

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

    public synchronized void close() {
        open = false;
        rawTasks.clear();
        mergedTasks.clear();
    }

    public boolean post(JsonObject content, boolean merge) {
        synchronized (this) {
            if (!open) {
                return false;
            }
            if (shouldQueue()) {
                addTask(content, merge);
                schedule(DISPATCH_DELAY);
                return false;
            } else if (merge && hasTasks()) {
                //Prevent out of order responses
                int rid = content.get("rid");
                JsonObject fromMerged = mergedTasks.get(rid);
                if (fromMerged != null) {
                    addTask(content, merge);
                    schedule(1);
                    return false;
                }
            }
        }
        JsonArray updates = new JsonArray();
        updates.add(content);
        forceWriteUpdates(updates);
        return true;
    }

    private void addTask(JsonObject content, boolean merge) {
        if (merge) {
            int rid = content.get("rid");
            JsonObject fromMerged = mergedTasks.get(rid);
            if (fromMerged == null) {
                mergedTasks.put(rid, content);
            } else {
                JsonArray oldUpdates = fromMerged.get("updates");
                if (oldUpdates != null) {
                    JsonArray newUpdates = content.remove("updates");
                    if (newUpdates != null) {
                        for (Object update : newUpdates) {
                            if (update instanceof JsonArray || update instanceof JsonObject) {
                                synchronized (fromMerged) {
                                    oldUpdates.add(update);
                                }
                            } else {
                                String clazz = update.getClass().getName();
                                String err = "Unhandled type: " + clazz;
                                throw new RuntimeException(err);
                            }
                        }
                        /* TODO trim backlog to prevent oom
                        synchronized (fromMerged) {
                            if (rid == 0) {
                                while (oldUpdates.size() > MAX_SID_BACKLOG) {
                                    oldUpdates.remove(0);
                                }
                            } else {
                                while (oldUpdates.size() > MAX_RID_BACKLOG) {
                                    oldUpdates.remove(0);
                                }
                            }
                        }
                        */
                    }
                    fromMerged.mergeIn(content);
                } else {
                    fromMerged.mergeIn(content);
                }
            }
            // TODO Debug logging
            int size = mergedTasks.size();
            if (size > (maxMergedSize + maxMergedSizeAdder)) {
                LOGGER.warn("New max merged tasks: " + size);
                maxMergedSize = size;
                maxMergedSizeAdder = Math.min((maxMergedSizeAdder * 2), 100000);
            }
        } else {
            rawTasks.add(content);
            // TODO Debug logging
            int size = rawTasks.size();
            if (size > (maxRawSize + maxRawSizeAdder)) {
                LOGGER.warn("New max raw tasks: " + size);
                maxRawSize = size;
                maxRawSizeAdder = Math.min((maxRawSizeAdder * 2), 100000);
            }
        }
    }

    private boolean hasTasks() {
        return !mergedTasks.isEmpty() || !rawTasks.isEmpty();
    }

    private JsonArray fetchUpdates() {
        if (!hasTasks()) {
            return null;
        }
        JsonArray updates = new JsonArray();
        Iterator<JsonObject> it = mergedTasks.values().iterator();
        int count = RID_CHUNK / 2;
        JsonObject obj;
        int rid;
        while (it.hasNext() && (--count >= 0)) {
            obj = it.next();
            rid = obj.get("rid");
            if (rid == 0) {
                JsonObject chunk = chunkSubUpdates(obj);
                if (chunk == null) {
                    it.remove();
                } else {
                    obj = chunk;
                }
            } else {
                it.remove();
            }
            updates.add(obj);
        }
        it = rawTasks.iterator();
        count += (RID_CHUNK / 2);
        while (it.hasNext() && (--count >= 0)) {
            updates.add(it.next());
            it.remove();
        }
        return updates;
    }

    @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
    public void run() {
        if (!open) {
            return;
        }
        JsonArray updates = null;
        synchronized (this) {
            fut = null;
            if (shouldQueue()) {
                //TODO Debug logging
                if (queueStarted < 0) {
                    queueStarted = System.currentTimeMillis();
                } else {
                    long duration = System.currentTimeMillis() - queueStarted;
                    if (duration > (maxQueueDuration + maxQueueDurationAdder)) {
                        LOGGER.warn("New max queueing duration: " + duration);
                        maxQueueDuration = duration;
                        maxQueueDurationAdder = Math.min((maxQueueDurationAdder * 2), 300000);
                    }
                }
            } else {
                updates = fetchUpdates();
            }
        }
        if (updates != null) {
            queueStarted = -1;
            forceWriteUpdates(updates);
        }
        synchronized (this) {
            if (hasTasks()) {
                if (shouldQueue()) {
                    schedule(DISPATCH_DELAY);
                } else {
                    schedule(1);
                }
            }
        }
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

    private void forceWrite(JsonObject obj) {
        if (!open) {
            return;
        }
        synchronized (writeMutex) {
            obj.put("msg", tracker.incrementMessageId());
            client.write(format, obj);
        }
    }

    private void forceWriteUpdates(JsonArray updates) {
        if (!open) {
            return;
        }
        JsonObject top = new JsonObject();
        top.put(topName, updates);
        forceWrite(top);
    }

    static {
        String s = PropertyReference.DISPATCH_DELAY;
        DISPATCH_DELAY = SystemPropertyUtil.getInt(s, 75);
        LOGGER.debug("-D{}: {}", s, DISPATCH_DELAY);
    }

    /**
     * Returns null if chunking is not necessary and the entire rid (the arg)
     * can be sent.
     */
    private JsonObject chunkSubUpdates(JsonObject fromMerged) {
        int size;
        JsonObject chunked = null;
        synchronized (fromMerged) {
            JsonArray orig = fromMerged.get("updates");
            size = orig.size();
            if (size > SID_CHUNK) {
                chunked = new JsonObject();
                chunked.put("rid", 0);
                JsonArray updates = new JsonArray();
                chunked.put("updates", updates);
                for (int i = 0; i < SID_CHUNK; i++) {
                    updates.add(orig.remove(0));
                }
            }
        }
        //TODO Debug logging
        if (size > (maxSidUpdates + maxSidUpdatesAdder)) {
            LOGGER.warn("New max sid updates: " + size);
            maxSidUpdates = size;
            maxSidUpdatesAdder = Math.min((maxSidUpdatesAdder * 2), 500000);
        }
        return chunked;
    }


}
