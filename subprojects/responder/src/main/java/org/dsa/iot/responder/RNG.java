package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Creates a random number generator
 * @author Samuel Grenier
 */
public class RNG {

    private static final Logger LOGGER;
    private static final ScheduledThreadPoolExecutor STPE;
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
            Node child = parent.getChild("rng_" + (max - 1));
            parent.removeChild(child);

            // Remove RNG task if possible
            ScheduledFuture<?> fut = futures.get(child);
            if (fut != null) {
                // Cancel out the RNG task
                fut.cancel(false);

                // Log removal
                final String path = child.getPath();
                final String msg = "Removed RNG child at " + path;
                LOGGER.info(msg);
            }
        }
    }

    private void setupRNG(final Node child) {
        ScheduledFuture<?> fut = STPE.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Value val = new Value(RANDOM.nextInt());
                child.setValue(val);

                int value = val.getNumber().intValue();
                LOGGER.info(child.getPath() + " has new value of " + value);
            }
        }, 0, 2, TimeUnit.SECONDS);
        futures.put(child, fut);
    }

    private synchronized int addAndGet(int count) {
        Value c = parent.getConfig("count");
        c = new Value(c.getNumber().intValue() + count);
        parent.setConfig("count", c);
        return c.getNumber().intValue();
    }

    private synchronized int getAndSubtract(int count) {
        Value prev = parent.getConfig("count");
        Value c = new Value(prev.getNumber().intValue() - count);
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
        STPE = new ScheduledThreadPoolExecutor(8, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
    }
}
