package org.dsa.iot.rng;

import lombok.val;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.dslink.client.ArgManager;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.responder.action.Action;
import org.dsa.iot.dslink.responder.action.Parameter;
import org.dsa.iot.dslink.util.Permission;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pshvets 
 */
public class Main {

    private static AtomicInteger counter = new AtomicInteger(0);
    private static final Random random = new Random();

    public static void main(String[] args) {
        // Create executor
        final val pool = new ScheduledThreadPoolExecutor(
                4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                val thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        });

        // DSLink creation
        val link = ArgManager.generate(args, "rng");

        // Create bus
        val generator = new Main();
        link.getBus().subscribe(generator);

        // Create parent Node
        val manager = link.getNodeManager();
        final val parent = manager.createRootNode("test");

        // Create Action for parent Node
        val actionNode = parent.createChild("generate");
        // Handler when "invoke" is called
        val action = new Action(Permission.READ,
                new org.vertx.java.core.Handler<JsonObject>() {

                    @Override
                    public void handle(JsonObject event) {
                        if (event == null) {
                            return;
                        }
                        // to measure execution time
                        long startTime = System.currentTimeMillis();

                        JsonObject params = event.getValue("params");
                        Integer numberOfNodes = params.getInteger("numbers");
                        Integer time = params.getInteger("time");

                        for (int i = 0; i < numberOfNodes; i++) {
                            Node a = parent.createChild("random_generated_"
                                    + counter.incrementAndGet());
                            a.setConfiguration("type", new Value(
                                    ValueType.NUMBER.toJsonString()));
                        }
                        System.out.println(numberOfNodes
                                + " nodes are generated");
                        if (time != null && time > 0) {
                            startPool(pool, parent, time, TimeUnit.MILLISECONDS);
                            System.out.println("timer changed to " + time);
                        }

                        long stopTime = System.currentTimeMillis();
                        long elapsedTime = stopTime - startTime;
                        System.out.println("Creation time " + elapsedTime);
                    }
                });
        action.addParameter(new Parameter("numbers", ValueType.NUMBER, null));
        action.addParameter(new Parameter("time", ValueType.NUMBER, null));
        actionNode.setAction(action);

        // Create variable
        val tuple = manager.getNode("test/random_generated_" + counter, true);
        val node = tuple.getKey();
        node.setConfiguration("type",
                new Value(ValueType.NUMBER.toJsonString()));

        startPool(pool, parent, 300, TimeUnit.MILLISECONDS);

        link.connect();
        link.sleep();
    }

    private static void startPool(ScheduledThreadPoolExecutor pool,
                                  final Node parent, int time, TimeUnit timeUnit) {
        pool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {

                Map<String, Node> children = parent.getChildren();
                if (children != null) {
                    for (Node node : children.values()) {
                        Value value = node.getValue();
                        if (value == null) {
                            value = new Value(0);
                        } else {
                            value = new Value(random.nextInt());
                        }
                        node.setValue(value);
                    }
                }

            }
        }, 0, time, timeUnit);
    }

    @Handler
    public void onConnected(ConnectedToServerEvent event) {
        System.out.println("RandomNumberGenerator is started");
    }
    
}
