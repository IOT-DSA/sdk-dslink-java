package org.dsa.iot.locker;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.engio.mbassy.listener.Filter;
import net.engio.mbassy.listener.Handler;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.client.ArgManager;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.events.ConnectedToServerEvent;
import org.dsa.iot.dslink.events.ResponseEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.requests.InvokeRequest;
import org.dsa.iot.dslink.requester.requests.ListRequest;
import org.dsa.iot.dslink.requester.requests.Request;
import org.dsa.iot.dslink.requester.requests.SubscribeRequest;
import org.dsa.iot.dslink.requester.responses.ListResponse;
import org.dsa.iot.dslink.requester.responses.SubscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

/**
 * 
 * 
 * @author pshvets
 *
 */
public class Main {
    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    private static DSLink link;

    /**
     * Used to track number of subscribed Points
     */
    AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) {
        Main m = new Main();
        link = ArgManager.generate(args, "requester", true, false);
        link.getBus().subscribe(m);

        link.connect();
        link.sleep();
    }

    /**
     * Called when DSLink connects to Broker
     * 
     * @param event
     *            that published to BUS
     */
    @Handler
    public synchronized void onConected(ConnectedToServerEvent event) {
        LOG.info("--------------");
        LOG.info("onConnected() method");

        // list request to "root" Node sent
        ListRequest request = new ListRequest("/");
        link.getRequester().sendRequest(event.getClient(), request);
        LOG.info("Sent data");
    }

    /**
     * Called when /list method response received from Broker and published into
     * the BUS
     * 
     * @param event
     */
    @Handler(filters = { @Filter(LockerRequesterFilter.ListResponseFilter.class) })
    public synchronized void onResponse(ResponseEvent event) {
        LOG.info("--------------");
        LOG.info("onResponse() method");
        LOG.info("Received response: {}", event.getName());
        ListResponse resp = (ListResponse) event.getResponse();
        NodeManager manager = resp.getManager();
        Node node = manager.getNode(resp.getPath()).getKey();
        LOG.info("Path: {}", resp.getPath());
        Map<String, Node> nodes = node.getChildren();
        if (nodes != null) {
            // Iterate through Tree of Nodes and call /list on every NOde
            // related to locker
            // All other Nodes are skipped
            for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                String name = entry.getKey();
                Node child = entry.getValue();

                // Lockers are under "cons"
                if (name.equals("conns")) {
                    sendRequestTo(new ListRequest(child.getPath()),
                            event.getClient());
                    break;
                }
                // Parent Node for all licker is found
                else if (name.equals("locker-a")) {
                    sendRequestTo(new ListRequest(child.getPath()),
                            event.getClient());
                    break;
                }
                // Called to get any data under each child locker
                else if (name.startsWith("locker")) {
                    sendRequestTo(new ListRequest(child.getPath()),
                            event.getClient());
                }
                // Now we traversed through tree and get to the Node we want to
                // subscribe.
                // This Node is called Point (Node with value to be subscribed)
                // Each Point has "type" parameter in configuration
                // The name of Point is "opened"
                else if (child.getConfiguration("type") != null) {
                    count.incrementAndGet();
                    LOG.info("Subscribed to : {}", child.getPath());
                    sendRequestTo(new SubscribeRequest(child.getPath()),
                            event.getClient());
                }
            }
        }
    }

    /**
     * Called when /subscribe method response received from Broker and published
     * into the BUS
     * 
     * @param event
     */
    @Handler(filters = { @Filter(LockerRequesterFilter.SubscriptionResponseFilter.class) })
    public synchronized void onSubscribeRequest(ResponseEvent event) {
        if (count.intValue() > 0) {
            LOG.info("--------------");
            LOG.info("onSubscribeRequest() method");
            LOG.info("Received response: {}", event.getName());

            SubscriptionResponse resp = (SubscriptionResponse) event
                    .getResponse();

            Iterator<Node> iterator = resp.getNodeList().iterator();
            while (iterator.hasNext()) {
                Node node = (Node) iterator.next();
                LOG.info("Node: {}", node.getPath());
                LOG.info("Value: {}", node.getValue().toDebugString());

                Node parent = node.getParent().get();
                if (parent != null) {
                    Map<String, Node> nodes = parent.getChildren();
                    if (nodes != null) {
                        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                            Node child = entry.getValue();

                            // Node that has an Action
                            if (child.getAction() != null) {
                                count.decrementAndGet();
                                Boolean value = node.getValue().getBool();
                                // Broker support "open" and "close" actions
                                // All locker can be opened from application,
                                // but
                                // only "locker2" can be closed from JAVA
                                // application
                                if (value == true
                                        && !parent.getName().equals("locker2")) {
                                    LOG.info("By contract from Broker only locker2 can be closed by application.");
                                    LOG.info("locker1 and locker3 can oly be opend");
                                } else {
                                    JsonObject object = new JsonObject();
                                    object.putBoolean("value", !value);
                                    LOG.info("Invoked: path:{}, value is:{}",
                                            child.getPath(), !value);
                                    sendRequestTo(
                                            new InvokeRequest(child.getPath(),
                                                    object), event.getClient());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when /invoke method response received from Broker and published
     * into the BUS
     * 
     * @param event
     */
    @Handler(filters = { @Filter(LockerRequesterFilter.InvokeResponseFilter.class) })
    public synchronized void onInvokeRequest(ResponseEvent event) {
        LOG.info("--------------");
        LOG.info("onInvokeRequest() method");
        LOG.info("Received response: {}", event.getName());

        InvokeRequest request = (InvokeRequest) event.getResponse()
                .getRequest();

        LOG.info("Name: {}", event.getName());
        LOG.info("Path: {}", request.getPath());
        LOG.info("Value: {}", request.getParams());
    }

    /**
     * Sends Request
     * 
     * @param request
     *            type of request
     * @param client
     *            type of client
     */
    private void sendRequestTo(Request request, Client client) {
        link.getRequester().sendRequest(client, request);
    }
}
