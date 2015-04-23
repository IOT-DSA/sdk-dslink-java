package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

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
public class RNG {

    private static final Logger LOGGER;
    private static final Random RANDOM = new Random();

    private final Node parent;
    private final Map<Node, ScheduledFuture<?>> futures;

    private RNG(Node parent) {
        this.parent = parent;
        this.futures = new ConcurrentHashMap<>();
    }

    public void initChildren() {
        Map<String, Node> children = parent.getChildren();
        if (children != null) {
            for (Node node : children.values()) {
                if (node.getAction() == null) {
                    setupRNG(node);
                }
            }
        }
    }

    private void addRNG(int count) {
        int max = addAndGet(count);
        int min = max - count;

        for (; min < max; min++) {
            // Setup child
            NodeBuilder builder = parent.createChild("rng_" + min);
            builder.setValue(new Value(0));
            final Node child = builder.build();

            // Log creation
            final String path = child.getPath();
            final String msg = "Created RNG child at " + path;
            LOGGER.info(msg);

            // Setup RNG
            setupRNG(child);
        }
    }

    private void removeRNG(int count) {
        int max = getAndSubtract(count);
        int min = max - count;

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
    }

    private void setupRNG(Node child) {
        child.getListener().addOnSubscribeHandler(new Handler<Node>() {
            @Override
            public void handle(final Node event) {
                LOGGER.info("Subscribed to {}", event.getPath());
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
        });

        child.getListener().addOnUnsubscribeHandler(new Handler<Node>() {
            @Override
            public void handle(Node event) {
                ScheduledFuture<?> fut = futures.remove(event);
                if (fut != null) {
                    fut.cancel(false);
                    LOGGER.info("Unsubscribed to {}", event.getPath());
                }
            }
        });
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
                .build();
        RNG rng = new RNG(parent);

        NodeBuilder builder = parent.createChild("addRNG");
        builder.setAction(getAddAction(rng));
        builder.build();

        builder = parent.createChild("removeRNG");
        builder.setAction(getRemoveAction(rng));
        builder.build();

        rng.initChildren();
    }

    private static Action getAddAction(final RNG rng) {
        Action act = new Action(Permission.READ, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                JsonObject params = event.getJsonIn().getObject("params");
                int count = 1;
                if (params != null) {
                    count = params.getInteger("count", 1);
                }
                if (count < 0) {
                    throw new IllegalArgumentException("count < 0");
                }
                rng.addRNG(count);
            }
        });
        act.addParameter(new Parameter("count", ValueType.NUMBER));
        return act;
    }

    private static Action getRemoveAction(final RNG rng) {
        Action act = new Action(Permission.READ, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                JsonObject params = event.getJsonIn().getObject("params");
                int count = 1;
                if (params != null) {
                    count = params.getInteger("count", 1);
                }
                if (count < 0) {
                    throw new IllegalArgumentException("count < 0");
                }
                rng.removeRNG(count);
            }
        });
        act.addParameter(new Parameter("count", ValueType.NUMBER));
        return act;
    }

    static {
        LOGGER =  LoggerFactory.getLogger(RNG.class);
    }
}
