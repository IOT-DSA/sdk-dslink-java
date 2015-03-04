package org.dsa.iot.responder;

import lombok.val;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.dslink.client.ArgManager;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            val thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    });

    public static void main(String[] args) {
        val main = new Main();
        val link = ArgManager.generate(args, "responder");
        link.getBus().subscribe(main);

        val manager = link.getNodeManager();
        val parent = manager.createRootNode("test");

        val a = parent.createChild("A");
        a.setConfiguration("test", new Value("Hello world"));
        a.setValue(new Value(1));

        val b = parent.createChild("B");
        b.setConfiguration("test", new Value("Hello there"));
        b.setValue(new Value(2));

        val tuple = manager.getNode("test/incremental", true);
        final val node = tuple.getKey();
        node.setConfiguration("type", new Value(ValueType.NUMBER.toJsonString()));
        main.pool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Value value = node.getValue();
                if (value == null) {
                    value = new Value(0);
                } else {
                    value = new Value(value.getInteger() + 1);
                }
                node.setValue(value);
                System.out.println("New incremental value: " + value.getInteger());
            }
        }, 0, 3, TimeUnit.SECONDS);

        link.connect();
        link.sleep();
    }

    @Handler
    public void onConnected(ConnectedToServerEvent event) {
        System.out.println("Connected");
    }
}
