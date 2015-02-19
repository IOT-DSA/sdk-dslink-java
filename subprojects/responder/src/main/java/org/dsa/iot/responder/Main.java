package org.dsa.iot.responder;

import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final MBassador<Event> bus = EventBusFactory.create();

    public static void main(String[] args) {
        Main m = new Main();
        m.bus.subscribe(m);
        m.run();
    }

    private void run() {
        val url = "http://localhost:8080/conn";
        val type = ConnectionType.WS;
        val dsId = "responder";
        val link = DSLink.generate(bus, url, type, dsId);

        val manager = link.getNodeManager();
        val parent = manager.createRootNode("test");

        val a = parent.createChild("A");
        a.setConfiguration("test", new Value("Hello world"));
        a.setValue(new Value(1));

        val b = parent.createChild("B");
        b.setConfiguration("test", new Value("Hello there"));
        b.setValue(new Value(2));

        link.connect();
        link.sleep();
    }

    @Handler
    public void onConnected(ConnectedToServerEvent event) {
        System.out.println("Connected");
    }
}
