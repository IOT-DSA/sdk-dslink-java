package org.dsa.iot.requester;

import lombok.SneakyThrows;
import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requester.requests.ListRequest;
import org.dsa.iot.dslink.requester.responses.ListResponse;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Main {

    private final MBassador<Event> bus = EventBusFactory.create();
    private DSLink link;

    public static void main(String[] args) {
        Main m = new Main();
        m.bus.subscribe(m);
        m.run();
    }

    @SneakyThrows
    private void run() {
        val url = "http://localhost:8080/conn";
        val type = ConnectionType.WS;
        val dsId = "requester";
        link = DSLinkFactory.create().generate(bus, url, type, dsId, true, false);
        link.connect();
        link.sleep();
        // TODO: it seems responder children display after multiple restarts of the requester
    }

    @Handler
    public synchronized void onConnected(ConnectedToServerEvent event) {
        System.out.println("--------------");
        System.out.println("Connected!");
        ListRequest request = new ListRequest("/");
        link.getRequester().sendRequest(event.getClient(), request);
        System.out.println("Sent data");
    }

    @Handler
    public synchronized void onResponse(ResponseEvent event) {
        System.out.println("--------------");
        System.out.println("Received response: " + event.getName());
        val resp = (ListResponse) event.getResponse();
        val manager = resp.getManager();
        val node = manager.getNode(resp.getPath()).getKey();
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
                String v = conf.getValue().toDebugString();
                if (indent) {
                    System.out.print("      ");
                }
                System.out.println(name + ": " + a + " => " + v);
            }
        }
    }
}
