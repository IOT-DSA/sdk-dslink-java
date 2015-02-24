package org.dsa.iot.responder;

import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.node.value.Value;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final MBassador<Event> bus = EventBusFactory.create();
    private final ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            val thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    });
    private DSLink link;
    
    public static void main(String[] args) {
        Main m = new Main();
        m.bus.subscribe(m);
        m.run();
    }

    private void run() {
        val url = "http://localhost:8080/conn";
        val type = ConnectionType.WS;
        val dsId = "responder";
        link = DSLinkFactory.create().generate(bus, url, type, dsId);

        val manager = link.getNodeManager();
        val parent = manager.createRootNode("test");

        val a = parent.createChild("A");
        a.setConfiguration("test", new Value("Hello world"));
        a.setValue(new Value(1));

        val b = parent.createChild("B");
        b.setConfiguration("test", new Value("Hello there"));
        b.setValue(new Value(2));

        pool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                val manager = link.getNodeManager();
                val tuple = manager.getNode("test/incremental", true);
                val node = tuple.getNode();
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
