package org.dsa.iot.requester;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requests.ListRequest;
import org.dsa.iot.dslink.responses.ListResponse;

import java.util.Map;

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
    public void onConnected(ConnectedToServerEvent event) {
        System.out.println("--------------");
        System.out.println("Connected!");
        ListRequest request = new ListRequest("/");
        link.getRequester().sendRequest(request);
        System.out.println("Sent data");
    }

    @Subscribe
    public void onResponse(ResponseEvent event) {
        System.out.println("--------------");
        System.out.println("Received response: " + event.getName());
        val resp = (ListResponse) event.getResponse();
        System.out.println("Path: " + resp.getPath());
        val nodes = resp.getManager().getChildren(resp.getPath());
        if (nodes != null) {
            System.out.println("Children: ");
            for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                String name = entry.getKey();
                Node node = entry.getValue();
                System.out.println("     Name: " + name);

                printValueMap(node.getAttributes(), "Attribute");
                printValueMap(node.getConfigurations(), "Configuration");

                // List children
                val req = new ListRequest(node.getPath());
                link.getRequester().sendRequest(req);
            }
        }
    }

    private void printValueMap(Map<String, Value> map, String name) {
        if (map != null) {
            for (Map.Entry<String, Value> conf : map.entrySet()) {
                String a = conf.getKey();
                String v = conf.getValue().toString();
                System.out.println("      " + name + ": " + a + " => " + v);
            }
        }
    }
}
