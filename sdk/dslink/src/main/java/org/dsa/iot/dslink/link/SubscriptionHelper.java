package org.dsa.iot.dslink.link;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.util.SubData;
import org.dsa.iot.dslink.util.handler.Handler;

/**
 * The requester api is written in such a way that multiple subscriptions to the same
 * path are not possible.  This uses a multiplexing adapter so that the requester
 * thinks there is only one subscriber.
 *
 * @author Aaron Hansen
 */
public class SubscriptionHelper {

    // Fields
    // ------

    private Requester requester;
    private ConcurrentHashMap<String, Adapter> subscriptions;

    // Constructors
    // ------------

    public SubscriptionHelper(Requester requester) {
        if (requester == null) {
            throw new NullPointerException("Requester must not be null");
        }
        this.requester = requester;
        subscriptions = new ConcurrentHashMap<String, Adapter>();
    }


    // Public Methods
    // --------------

    /**
     * Clears all subscriptions, but does not call unsubscribe on the requester.
     */
    public void clear() {
        if (subscriptions != null) {
            subscriptions.clear();
        }
    }

    /**
     * Clears a single subscription without calling unsubscribe on the requester.
     */
    public SubscriptionHelper clear(String path) {
        Adapter a = subscriptions.remove(path);
        if (a != null) {
            requester.unsubscribe(path, null);
        }
        return this;
    }

    /**
     * Safely subscribes the path, even if it is already subscribed.  Calls
     * subscribe with a SubData for the given path and a QOS of 0.
     */
    public SubscriptionHelper subscribe(String path,
                                        Handler<SubscriptionValue> handler) {
        return subscribe(new SubData(path, 0), handler);
    }

    /**
     * Safely subscribes the path, even if it is already subscribed.  The path will
     * be subscribed at the give QOS of the very first subscription.  Subsequent
     * subscriptions will get the same QOS.
     *
     * @param path The first scription
     */
    public SubscriptionHelper subscribe(SubData path,
                                        Handler<SubscriptionValue> handler) {
        Adapter a = subscriptions.get(path.getPath());
        if (a == null) {
            Adapter adapter = new Adapter();
            adapter.add(handler);
            requester.subscribe(path, adapter);
            subscriptions.put(path.getPath(), adapter);
        } else {
            a.add(handler);
        }
        return this;
    }

    /**
     * Only unsubscribes the given handler.
     */
    public SubscriptionHelper unsubscribe(String path,
                                          Handler<SubscriptionValue> handler) {
        Adapter a = subscriptions.get(path);
        if (a != null) {
            a.remove(handler);
            if (a.size() == 0) {
                requester.unsubscribe(path, null);
                subscriptions.remove(path);
            }
        }
        return this;
    }

    /**
     * Only unsubscribes the given handler.
     */
    public SubscriptionHelper unsubscribe(SubData path,
                                          Handler<SubscriptionValue> handler) {
        return unsubscribe(path.getPath(),handler);
    }

    /**
     * Unsubscribe all handlers for the given path.
     */
    public SubscriptionHelper unsubscribeAll(String path) {
        Adapter a = subscriptions.remove(path);
        if (a != null) {
            requester.unsubscribe(path, null);
        }
        return this;
    }


    // Inner Classes
    // -------------

    /**
     * Only identical instances are equal (==).
     */
    private static class HandlerComparator
            implements Comparator<Handler<SubscriptionValue>> {

        public int compare(Handler<SubscriptionValue> o1, Handler<SubscriptionValue> o2) {
            if (o1 == o2) {
                return 0;
            }
            if (System.identityHashCode(o1) <= System.identityHashCode(o2)) {
                return -1;
            }
            return 1;
        }

    }

    /**
     * A subscription value handler that multiplexes callbacks.
     */
    private static class Adapter implements Handler<SubscriptionValue> {

        private static HandlerComparator comparator = new HandlerComparator();
        private ConcurrentSkipListSet<Handler<SubscriptionValue>> handlers = null;

        Adapter() {
            handlers = new ConcurrentSkipListSet<Handler<SubscriptionValue>>(comparator);
        }


        Adapter add(Handler<SubscriptionValue> handler) {
            if (!handlers.contains(handler)) {
                handlers.add(handler);
            }
            return this;
        }

        public void handle(SubscriptionValue value) {
            for (Handler<SubscriptionValue> handler : handlers) {
                try {
                    handler.handle(value);
                } catch (Exception x) {
                    Logger.getGlobal().log(Level.WARNING, value.toString(), value);
                }
            }
        }

        void remove(Handler<SubscriptionValue> handler) {
            handlers.remove(handler);
        }

        int size() {
            return handlers.size();
        }

    }
}
