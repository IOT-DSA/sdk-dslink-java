package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionRegistry;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Creates a random number generator
 * @author Samuel Grenier
 */
public class RNG {

    private static final Logger LOGGER;
    private static final ScheduledThreadPoolExecutor STPE;
    private static final Map<Node, RNG> NODES;

    private static final Random RANDOM = new Random();
    private static final Object LOCK = new Object();

    private final Node node;
    private ScheduledFuture<?> fut;

    public RNG(Node node) {
        this.node = node;
    }

    private void start() {
        fut = STPE.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Value val = new Value(RANDOM.nextInt());
                node.setValue(val);
                LOGGER.info(node.getPath() + " has new value of " + val.getNumber().intValue());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void stop() {
        if (fut != null) {
            fut.cancel(false);
        }
    }

    public static void addActions(ActionRegistry registry) {
        Permission p = Permission.READ;
        {
            Action action = new Action("addRNG", p, new RNG.AddHandler());
            action.addParameter(new Parameter("count", ValueType.NUMBER));
            registry.register(action);
        }

        {
            Action action = new Action("removeRNG", p, new RNG.RemoveChildrenHandler());
            action.addParameter(new Parameter("count", ValueType.NUMBER));
            registry.register(action);
        }
    }

    public static void init(Node parent) {
        NodeBuilder builder = parent.createChild("rng");
        builder.setAttribute("count", new Value(0));
        Node child = builder.build();

        String name = "addRNG";
        builder = child.createChild(name);
        builder.setAction(name);
        builder.build();

        name = "removeRNG";
        builder = child.createChild(name);
        builder.setAction(name);
        builder.build();

        Map<String, Node> children = child.getChildren();
        for (Node node : children.values()) {
            if (node.getAction() == null) {
                RNG rng = new RNG(node);
                rng.start();
                NODES.put(node, rng);
            }
        }
    }

    public static Node addRNG(String name, Node parent) {
        NodeBuilder builder = parent.createChild(name);
        builder.setValue(new Value(0));
        Node node = builder.build();

        RNG rng = new RNG(node);
        rng.start();
        NODES.put(node, rng);

        LOGGER.info("Created RNG child at " + node.getPath());
        return node;
    }

    public static class AddHandler implements Handler<ActionResult> {
        @Override
        public void handle(ActionResult event) {
            int incrementCount = 1;

            JsonObject params = event.getJsonIn().getObject("params");
            if (params != null) {
                Integer count = params.getInteger("count");
                if (count != null) {
                    incrementCount = count;
                }
            }

            Node node = event.getNode().getParent();
            synchronized (LOCK) {
                Value value = node.getAttribute("count");
                int min = value.getNumber().intValue();
                int max = min + incrementCount;
                node.setAttribute("count", new Value(max));

                for (int i = min; i < max; i++) {
                    Node child = addRNG("rng_" + i, node);
                    LOGGER.info("Created RNG child at " + child.getPath());
                }
            }
        }
    }

    public static class RemoveChildrenHandler implements Handler<ActionResult> {

        @Override
        public void handle(ActionResult event) {
            int decrementCount = 1;

            JsonObject params = event.getJsonIn().getObject("params");
            if (params != null) {
                Integer count = params.getInteger("count");
                if (count != null) {
                    decrementCount = count;
                }
            }

            Node node = event.getNode().getParent();
            synchronized (LOCK) {
                Value value = node.getAttribute("count");
                int max = value.getNumber().intValue();
                int min = max - decrementCount;
                if (min < 0)
                    min = 0;
                node.setAttribute("count", new Value(min));

                for (int i = max; i > min; i--) {
                    Node child = node.removeChild("rng_" + (i - 1));
                    RNG rng = NODES.remove(child);
                    rng.stop();
                    LOGGER.info("Removed RNG child at " + child.getPath());
                }
            }
        }
    }

    static {
        LOGGER =  LoggerFactory.getLogger(RNG.class);
        NODES = new HashMap<>();
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
