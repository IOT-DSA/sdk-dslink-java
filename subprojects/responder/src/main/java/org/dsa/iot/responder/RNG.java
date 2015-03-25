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

        builder = child.createChild("addRNG");
        builder.setAction("addRNG");
        builder.build();

        { // Pre-inject 1 rng
            RNG rng = new RNG("rng_1", child, child.getLink());
            child.addChild(rng);
            rng.start();
            LOGGER.info("Created RNG child at " + rng.getPath());

            builder = rng.createChild("remove");
            builder.setAction("removeRNG");
            builder.build();
        }
    }

    public static Handler<ActionResult> getAddHandler() {
        return new Handler<ActionResult>() {

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
                Value value = node.getAttribute("count");
                int min = value.getNumber().intValue();
                int max = min + incrementCount;
                node.setAttribute("count", new Value(max));

                for (int i = min; i < max; i++) {
                    RNG child = new RNG("rng_" + i, node, node.getLink());
                    node.addChild(child);
                    child.start();
                    LOGGER.info("Created RNG child at " + child.getPath());

                    NodeBuilder builder = child.createChild("remove");
                    builder.setAction("removeRNG");
                    builder.build();

                    // TODO: dynamically changing numbers
                }
            }
        };
    }

    public static Handler<ActionResult> getRemoveHandler() {
        return new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Node child = event.getNode().getParent();
                if (child != null) {
                    Node parent = child.getParent();
                    if (parent != null) {
                        RNG rng = (RNG) parent.removeChild(child);
                        rng.stop();
                        LOGGER.info("Removed RNG child at " + child.getPath());
                    }
                }
            }
        };
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
