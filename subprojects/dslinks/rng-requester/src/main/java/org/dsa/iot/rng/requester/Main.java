package org.dsa.iot.rng.requester;

import java.util.Map;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Filter;
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
import org.dsa.iot.dslink.requester.responses.SubscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pshvets
 *
 */
public class Main {
    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    private static DSLink link;

    public static void main(String[] args) {
        Main m = new Main();

        MBassador<Event> bus = EventBusFactory.create();
        bus.subscribe(m);

        // Port of broker to be connected
        String url = "http://localhost:8080/conn";

        DSLinkFactory factory = DSLinkFactory.create();

        link = factory.generate(bus, url, ConnectionType.WS, "requester", true,
                false);

        link.connect();
        link.sleep();
    }

    @Handler
    public synchronized void onConnected(ConnectedToServerEvent event) {
        LOG.info("--------------");
        LOG.info("Connected!");

        ListRequest request = new ListRequest("/");
        link.getRequester().sendRequest(event.getClient(), request);
        LOG.info("Sent data");
    }

    @Handler(filters = { @Filter(RngRequesterFilter.ListResponseFilter.class) })
    public synchronized void onResponse(ResponseEvent event) {
        LOG.info("--------------");
        LOG.info("Received response: " + event.getName());
        ListResponse resp = (ListResponse) event.getResponse();
        NodeManager manager = resp.getManager();
        Node node = manager.getNode(resp.getPath()).getKey();
        LOG.info("Path: " + resp.getPath());
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

    @Handler(filters = { @Filter(RngRequesterFilter.SubscriptionResponseFilter.class) })
    public void onSubscribeRequest(ResponseEvent event) {
        LOG.info("--------------");
        LOG.info("Received subscription response` response: " + event.getName());
        SubscriptionResponse resp = (SubscriptionResponse) event.getResponse();
        LOG.info(resp.toString());
    }

    private void sendRequestTo(Request request, Client client) {
        link.getRequester().sendRequest(client, request);
    }
}
