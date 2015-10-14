package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.util.handler.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages subscriptions in the historian.
 *
 * @author Samuel Grenier
 */
public class SubscriptionPool {

    private final Map<String, SubHandler> subscriptions = new HashMap<>();
    private final Requester requester;

    public SubscriptionPool(Requester requester) {
        this.requester = requester;
    }

    public synchronized void subscribe(String path, Watch watch) {
        SubHandler handler = subscriptions.get(path);
        if (handler == null) {
            handler = new SubHandler();
            subscriptions.put(path, handler);
            handler.addWatch(watch);
            requester.subscribe(path, handler);
        } else {
            handler.addWatch(watch);
        }
    }

    public synchronized void unsubscribe(String path, Watch watch) {
        SubHandler handler = subscriptions.get(path);
        if (handler != null) {
            handler.removeWatch(watch);
            if (handler.isEmpty()) {
                requester.unsubscribe(path, null);
                subscriptions.remove(path);
            }
        }
    }

    private static class SubHandler implements Handler<SubscriptionValue> {

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final List<Watch> watches = new ArrayList<>();

        public boolean isEmpty() {
            lock.readLock().lock();
            try {
                return watches.isEmpty();
            } finally {
                lock.readLock().unlock();
            }
        }

        public void addWatch(Watch watch) {
            lock.writeLock().lock();
            try {
                if (!watches.contains(watch)) {
                    watches.add(watch);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void removeWatch(Watch watch) {
            lock.writeLock().lock();
            try {
                watches.remove(watch);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public void handle(SubscriptionValue event) {
            lock.readLock().lock();
            try {
                for (Watch w : watches) {
                    w.onData(event);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
