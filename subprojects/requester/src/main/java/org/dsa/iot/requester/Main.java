package org.dsa.iot.requester;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.events.IncomingDataEvent;
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
        link.getRequester().sendRequest(event.getClient(), request);
        System.out.println("Sent data");
    }

    @Subscribe
    public void onData(IncomingDataEvent event) {
        System.out.println(event.getData().encode()); // DEBUG
    }

    @Subscribe
    public void onResponse(ResponseEvent event) {
        System.out.println("--------------");
        System.out.println("Received response: " + event.getName());
        val resp = (ListResponse) event.getResponse();
        val manager = resp.getManager();
        val node = manager.getNode(resp.getPath()).getNode();
        System.out.println("Path: " + resp.getPath());
        printValueMap(node.getAttributes(), "Attribute", false);
        printValueMap(node.getConfigurations(), "Configuration", false);
        val nodes = node.getChildren();
        if (nodes != null) {
            System.out.println("Children: ");
            for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                String name = entry.getKey();
                Node child = entry.getValue();
                System.out.println("    Name: " + name);

                printValueMap(child.getAttributes(), "Attribute", true);
                printValueMap(child.getConfigurations(), "Configuration", true);

                // List children
                val req = new ListRequest(child.getPath());
                link.getRequester().sendRequest(event.getClient(), req);
            }
        }
    }

    private void printValueMap(Map<String, Value> map, String name, boolean indent) {
        if (map != null) {
            for (Map.Entry<String, Value> conf : map.entrySet()) {
                String a = conf.getKey();
                String v = conf.getValue().toString();
                if (indent) {
                    System.out.print("      ");
                }
                System.out.println(name + ": " + a + " => " + v);
            }
        }
    }
}
