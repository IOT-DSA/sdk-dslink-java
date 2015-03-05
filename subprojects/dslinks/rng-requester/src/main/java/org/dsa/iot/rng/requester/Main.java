package org.dsa.iot.rng.requester;

import java.util.Map;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;

import org.dsa.iot.core.event.Event;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.requests.ListRequest;
import org.dsa.iot.dslink.requester.requests.Request;
import org.dsa.iot.dslink.requester.requests.SubscribeRequest;
import org.dsa.iot.dslink.requester.responses.ListResponse;

/**
 * @author pshvets
 *
 */
public class Main {

    private static DSLink link;

    public static void main(String[] args) {
        Main m = new Main();

        MBassador<Event> bus = EventBusFactory.create();
        bus.subscribe(m);

        String url = "http://localhost:8080/conn";

        DSLinkFactory factory = DSLinkFactory.create();

        link = factory.generate(bus, url, ConnectionType.WS, "requester", true,
                false);

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
        if (event.getResponse() instanceof ListResponse) {
            ListResponse resp = (ListResponse) event.getResponse();
            NodeManager manager = resp.getManager();
            Node node = manager.getNode(resp.getPath()).getKey();
            System.out.println("Path: " + resp.getPath());
            Map<String, Node> nodes = node.getChildren();
            if (nodes != null) {
                for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                    String name = entry.getKey();
                    Node child = entry.getValue();
                    if (name.equals("conns")) {
                        sendRequestTo(new ListRequest(child.getPath()),
                                event.getClient());
                    } else if (name.startsWith("broker")) {
                        sendRequestTo(new ListRequest(child.getPath()),
                                event.getClient());
                    } else if (name.startsWith("rng")) {
                        sendRequestTo(new ListRequest(child.getPath()),
                                event.getClient());
                    } else if (name.startsWith("test")) {
                        sendRequestTo(new ListRequest(child.getPath()),
                                event.getClient());
                    } else if (name.startsWith("random")) {
                        sendRequestTo(new SubscribeRequest(child.getPath()),
                                event.getClient());
                    }
                }
            }
        }
    }

    private void sendRequestTo(Request request, Client client) {
        link.getRequester().sendRequest(client, request);
    }
}
