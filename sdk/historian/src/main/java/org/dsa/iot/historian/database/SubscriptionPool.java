package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.methods.requests.RemoveRequest;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.SubData;
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

    private static final int QOS = 1;
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
            requester.subscribe(new SubData(path, QOS), handler);
        } else {
            handler.addWatch(watch);
        }
    }

    public synchronized void unsubscribe(String path, Watch watch) {
        SubHandler handler = subscriptions.get(path);
        if (handler != null) {
            handler.removeWatch(watch);
            if (handler.isEmpty()) {
                String getHistoryActionAliasPath = path + "/@@getHistory";

                requester.unsubscribe(path, handler, null);
                requester.remove(new RemoveRequest(getHistoryActionAliasPath), null);
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
                Value val = event.getValue();
                if (val == null) {
                    return;
                }
                try {
                    if ((val.getTime() < 0) && !val.isImmutable()) {
                        val.setTime(System.currentTimeMillis());
                    }
                } catch (Exception x) {
                    //Just in case there are parsing errors because of wacky
                    //timestamp formatting.
                    val.setTime(System.currentTimeMillis());
                }
                for (Watch w : watches) {
                    w.onData(event);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
