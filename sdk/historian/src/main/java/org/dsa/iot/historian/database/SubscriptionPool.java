package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.vertx.java.core.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public synchronized void clear() {
        subscriptions.clear();
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

        private final List<Watch> watches = new ArrayList<>();

        public synchronized boolean isEmpty() {
            return watches.isEmpty();
        }

        public synchronized void addWatch(Watch watch) {
            watches.add(watch);
        }

        public synchronized void removeWatch(Watch watch) {
            watches.remove(watch);
        }

        @Override
        public synchronized void handle(SubscriptionValue event) {
            for (Watch w : watches) {
                w.onData(event);
            }
        }
    }
}
