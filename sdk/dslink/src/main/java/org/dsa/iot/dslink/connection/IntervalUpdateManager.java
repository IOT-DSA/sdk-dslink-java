package org.dsa.iot.dslink.connection;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles updates to the remote endpoint that is not allowed to exceed the
 * minimum update interval.
 *
 * @author Samuel Grenier
 */
public class IntervalUpdateManager {

    private final Map<Integer, JsonObject> tasks = new HashMap<>();
    private final Handler<Collection<JsonObject>> callback;
    private final int updateInterval;

    private long time;
    private ScheduledFuture<?> fut;

    public IntervalUpdateManager(int updateInterval, Handler<Collection<JsonObject>> callback) {
        if (callback == null) {
            throw new NullPointerException("callback");
        }
        this.updateInterval = updateInterval;
        this.callback = callback;
    }

    public synchronized void post(JsonObject content) {
        List<JsonObject> list = new ArrayList<>(1);
        list.add(content);
        post(list);
    }

    public synchronized void post(List<JsonObject> content) {
        long curr = System.currentTimeMillis();
        long diff = curr - time;
        if (diff > updateInterval) {
            callback.handle(content);
        } else {
            addTask(content);
            long delay = updateInterval - diff;
            if (fut == null) {
                fut = org.dsa.iot.dslink.util.Objects.getDaemonThreadPool().schedule(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (IntervalUpdateManager.this) {
                            if (!tasks.isEmpty()) {
                                callback.handle(tasks.values());
                                tasks.clear();
                            }
                            fut = null;
                        }
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
        }
        time = curr;
    }

    private void addTask(List<JsonObject> content) {
        for (JsonObject obj : content) {
            addTask(obj);
        }
    }

    @SuppressWarnings("unchecked")
    private void addTask(JsonObject content) {
        int rid = content.getInteger("rid");
        JsonObject obj = tasks.get(rid);
        if (obj == null) {
            tasks.put(rid, content);
        } else {
            JsonArray oldUpdates = obj.getField("updates");
            if (oldUpdates != null) {
                Object newUpdates = content.removeField("updates");
                if (newUpdates instanceof List) {
                    for (Object update : (List) newUpdates) {
                        if (update instanceof List) {
                            oldUpdates.add(new JsonArray((List) update));
                        } else if (update instanceof Map) {
                            oldUpdates.add(new JsonObject((Map) update));
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
}
