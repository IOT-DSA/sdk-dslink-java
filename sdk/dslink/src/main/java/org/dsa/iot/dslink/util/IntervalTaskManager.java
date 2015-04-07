package org.dsa.iot.dslink.util;

import org.vertx.java.core.Handler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Handles tasks that have a minimum update interval that is not allowed
 * to be exceeded. If the update interval is exceeded then a delay will
 * be triggered to wait until the update interval time is met.
 *
 * @author Samuel Grenier
 */
public class IntervalTaskManager<T> {

    private final ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(1);

    private final List<T> tasks = new LinkedList<>();
    private final Handler<List<T>> callback;
    private final int updateInterval;

    private long time;
    private ScheduledFuture<?> fut;

    public IntervalTaskManager(int updateInterval, Handler<List<T>> callback) {
        if (callback == null) {
            throw new NullPointerException("callback");
        }
        this.updateInterval = updateInterval;
        this.callback = callback;
    }

    public synchronized void post(T content) {
        List<T> list = new ArrayList<>(1);
        list.add(content);
        post(list);
    }

    public synchronized void post(List<T> content) {
        long curr = System.currentTimeMillis();
        long diff = curr - time;
        if (diff > updateInterval) {
            callback.handle(content);
        } else {
            tasks.addAll(content);
            long delay = updateInterval - diff;
            if (fut == null) {
                fut = stpe.schedule(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (IntervalTaskManager.this) {
                            if (!tasks.isEmpty()) {
                                callback.handle(tasks);
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
}
