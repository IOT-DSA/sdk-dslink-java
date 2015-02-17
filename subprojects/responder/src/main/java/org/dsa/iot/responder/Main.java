package org.dsa.iot.responder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final EventBus bus = new EventBus();

    public static void main(String[] args) {
        Main m = new Main();
        m.bus.register(m);
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

    @Subscribe
    public void onConnected(ConnectedToServerEvent event) {
        System.out.println("Connected");
    }
}
