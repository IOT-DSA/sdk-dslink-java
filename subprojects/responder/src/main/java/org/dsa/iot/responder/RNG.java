package org.dsa.iot.responder;

import org.dsa.iot.dslink.link.Linkable;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Creates a random number generator
 * @author Samuel Grenier
 */
public class RNG extends Node {

    private static final Logger LOGGER;
    private static final ScheduledThreadPoolExecutor STPE;
    private static final Random RANDOM = new Random();
    private static final Object LOCK = new Object();

    private ScheduledFuture<?> fut;

    public RNG(String name, Node parent, Linkable link) {
        super(name, parent, link);
    }

    private void start() {
        fut = STPE.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Value val = new Value(RANDOM.nextInt());
                setValue(val);
                LOGGER.info(getPath() + " has new value of " + val.getNumber().intValue());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void stop() {
        if (fut != null) {
            fut.cancel(false);
        }
    }

    public static void start(Node parent) {
        NodeBuilder builder = parent.createChild("rng");
        builder.setAttribute("count", new Value(1));
        Node child = builder.build();

        String name = "addRNG";
        builder = child.createChild(name);
        builder.setAction(name);
        builder.build();

        name = "removeRNG";
        builder = child.createChild(name);
        builder.setAction(name);
        builder.build();

        addRNG("rng_0", child);
    }

    public static Node addRNG(String name, Node parent) {
        RNG rng = new RNG(name, parent, parent.getLink());
        parent.addChild(rng);
        rng.start();
        LOGGER.info("Created RNG child at " + rng.getPath());
        return rng;
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
                    RNG rng = (RNG) node.removeChild("rng_" + (i - 1));
                    rng.stop();
                    LOGGER.info("Removed RNG child at " + rng.getPath());
                }
            }
        }
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
