package org.dsa.iot.requester;

import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;
import net.engio.mbassy.listener.Handler;

import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.client.ArgManager;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requester.requests.ListRequest;
import org.dsa.iot.dslink.requester.responses.ListResponse;

/**
 * @author Samuel Grenier
 */
public class Main {

    private DSLink link;

    public static void main(String[] args) {
        Main m = new Main();
        m.run(args);
    }

    @SneakyThrows
    private void run(String[] args) {
        val bus = EventBusFactory.create();
        bus.subscribe(this);
        link = ArgManager.generateRequester(args, bus, "requester");
        link.connect();
        link.sleep();
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

    private void printValueMap(Map<String, Value> map, String name,
            boolean indent) {
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
