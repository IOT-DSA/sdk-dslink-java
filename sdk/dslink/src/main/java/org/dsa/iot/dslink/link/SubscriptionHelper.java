package org.dsa.iot.dslink.link;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dsa.iot.dslink.methods.responses.UnsubscribeResponse;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.util.SubData;
import org.dsa.iot.dslink.util.handler.Handler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    public SubscriptionHelper clear() {
        subscriptions.clear();
        return this;
    }

    /**
     * Clears a single subscription without calling unsubscribe on the requester.
     */
    public SubscriptionHelper clear(String path) {
        subscriptions.remove(path);
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
    @SuppressFBWarnings("AT_OPERATION_SEQUENCE_ON_CONCURRENT_ABSTRACTION")
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
     *
     * @param response Can be null.
     */
    @SuppressFBWarnings("AT_OPERATION_SEQUENCE_ON_CONCURRENT_ABSTRACTION")
    public SubscriptionHelper unsubscribe(String path,
                                          Handler<SubscriptionValue> handler,
                                          Handler<UnsubscribeResponse> response) {
        Adapter a = subscriptions.get(path);
        if (a != null) {
            a.remove(handler);
            if (a.size() == 0) {
                requester.unsubscribe(path, response);
                subscriptions.remove(path);
            }
        }
        return this;
    }

    /**
     * Only unsubscribes the given handler.
     *
     * @param response Can be null.
     */
    public SubscriptionHelper unsubscribe(SubData path,
                                          Handler<SubscriptionValue> handler,
                                          Handler<UnsubscribeResponse> response) {
        return unsubscribe(path.getPath(), handler, response);
    }

    /**
     * Unsubscribe all handlers for the given path.
     *
     * @param response Can be null.
     */
    public SubscriptionHelper unsubscribeAll(
            String path, Handler<UnsubscribeResponse> response) {
        Adapter a = subscriptions.remove(path);
        if (a != null) {
            requester.unsubscribe(path, response);
        }
        return this;
    }


    // Inner Classes
    // -------------

    /**
     * Only identical instances are equal (==).
     */
    @SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
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
