package org.dsa.iot.responder.rng;

import org.dsa.iot.dslink.node.MetaData;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.NodeListener;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Creates a random number generator
 *
 * @author Samuel Grenier
 */
public class RNG implements MetaData {

    private static final Random RANDOM = new Random();
    private static final Logger LOGGER;

    private final Map<Node, ScheduledFuture<?>> futures;
    private Node parent;

    private RNG() {
        this.futures = new ConcurrentHashMap<>();
    }

    @Override
    public void setNode(Node node) {
        this.parent = node;
    }

    public void initChildren() {
        Map<String, Node> children = parent.getChildren();
        if (children != null) {
            for (Node node : children.values()) {
                if (node.getAction() == null) {
                    setupRNG(node.createFakeBuilder());
                }
            }
        }
    }

    int addRNG(int count) {
        int max = addAndGet(count);
        int min = max - count;

        for (; min < max; min++) {
            // Setup child
            NodeBuilder builder = parent.createChild("rng_" + min);
            setupRNG(builder);
            builder.setValueType(ValueType.NUMBER);
            builder.setValue(new Value(0));
            final Node child = builder.build();

            // Log creation
            final String path = child.getPath();
            final String msg = "Created RNG child at " + path;
            LOGGER.info(msg);
        }
        return max;
    }

    int removeRNG(int count) {
        int max = getAndSubtract(count);
        int min = max - count;
        if (min < 0) {
            min = 0;
        }

        for (; max > min; max--) {
            // Remove child if possible
            Node child = parent.removeChild("rng_" + (max - 1));
            if (child == null) {
                continue;
            }

            // Log removal
            final String path = child.getPath();
            final String msg = "Removed RNG child at " + path;
            LOGGER.info(msg);

            // Remove RNG task if possible
            ScheduledFuture<?> fut = futures.remove(child);
            if (fut != null) {
                // Cancel out the RNG task
                fut.cancel(false);
            }
        }
        return min;
    }

    void subscribe(final Node event) {
        LOGGER.info("Subscribed to {}", event.getPath());
        if (futures.containsKey(event)) {
            return;
        }
        ScheduledThreadPoolExecutor stpe = Objects.getDaemonThreadPool();
        ScheduledFuture<?> fut = stpe.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Value val = new Value(RANDOM.nextInt());
                event.setValue(val);

                int value = val.getNumber().intValue();
                LOGGER.info(event.getPath() + " has new value of " + value);
            }
        }, 0, 2, TimeUnit.SECONDS);
        futures.put(event, fut);
    }

    void unsubscribe(final Node event) {
        ScheduledFuture<?> fut = futures.remove(event);
        if (fut != null) {
            fut.cancel(false);
            LOGGER.info("Unsubscribed to {}", event.getPath());
        }
    }

    private void setupRNG(NodeBuilder child) {
        NodeListener listener = child.getListener();
        listener.setOnSubscribeHandler(Actions.getSubHandler());
        listener.setOnUnsubscribeHandler(Actions.getUnsubHandler());
    }

    private synchronized int addAndGet(int count) {
        Value c = parent.getConfig("count");
        c = new Value(c.getNumber().intValue() + count);
        parent.setConfig("count", c);
        return c.getNumber().intValue();
    }

    private synchronized int getAndSubtract(int count) {
        Value prev = parent.getConfig("count");
        count = prev.getNumber().intValue() - count;
        if (count < 0)
            count = 0;
        Value c = new Value(count);
        parent.setConfig("count", c);
        return prev.getNumber().intValue();
    }

    public static void init(Node superRoot) {
        Node parent = superRoot.createChild("rng")
                .setConfig("count", new Value(0))
                .setMetaData(new RNG())
                .build();
        RNG rng = parent.getMetaData();

        NodeBuilder builder = parent.createChild("addRNG");
        builder.setAction(Actions.getAddAction());
        builder.build();

        builder = parent.createChild("removeRNG");
        builder.setAction(Actions.getRemoveAction());
        builder.build();

        builder = parent.createChild("tableStream");
        builder.setAction(Actions.getTableStreamAction());
        builder.build();

        builder = parent.createChild("tableRefresh");
        builder.setAction(Actions.getTableRefreshAction());
        builder.build();

        builder = parent.createChild("tableReplace");
        builder.setAction(Actions.getTableReplaceAction());
        builder.build();

        rng.initChildren();
    }

    static {
        LOGGER =  LoggerFactory.getLogger(RNG.class);
    }
}
