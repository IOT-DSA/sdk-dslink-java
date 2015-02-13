package org.dsa.iot.requester;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.requests.ListRequest;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final EventBus bus = new EventBus();
    private DSLink link;

    public static void main(String[] args) {
        Main m = new Main();
        m.bus.register(m);
        m.run();
    }

    private void run() {
        val url = "http://localhost:8080/conn";
        val type = ConnectionType.WS;
        val dsId = "requester";
        link = DSLink.generate(bus, url, type, dsId, true, false);
        link.connect();
        link.sleep();
    }

    @Subscribe
    public void onConnected(ConnectedToServerEvent e) {
        System.out.println("Connected!");
        ListRequest request = new ListRequest("/");
        link.getRequester().sendRequest(request);
        System.out.println("Sent data");
    }
}
