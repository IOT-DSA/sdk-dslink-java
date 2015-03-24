package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles subscriptions for values and paths.
 * @author Samuel Grenier
 */
public class SubscriptionManager {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private final Map<Node, ListResponse> pathSubs = new HashMap<>();
    private final Map<Node, Integer> valueSubsNodes = new HashMap<>();
    private final Map<Integer, Node> valueSubsSids = new HashMap<>();
    private final DSLink link;

    public SubscriptionManager(DSLink link) {
        this.link = link;
    }

    /**
     * Adds a value subscription to the designated node. This will allow a node
     * to publish a value update and have it updated to the remote endpoint if
     * it is subscribed.
     *
     * @param node Node to subscribe to
     * @param sid Subscription ID to send back to the client
     */
    public synchronized void addValueSub(Node node, int sid) {
        Integer prev = valueSubsNodes.put(node, sid);
        valueSubsSids.put(sid, node);
        if (prev != null) {
            throw new NullPointerException("Node " + node.getPath() + " already subscribed");
        }
    }

    /**
     * Removes a value subscription from the designated node. The remote
     * endpoint will no longer receive updates when the value updates.
     *
     * @param sid Subscription ID to unsubscribe
     */
    public synchronized void removeValueSub(int sid) {
        Node node = valueSubsSids.remove(sid);
        valueSubsNodes.remove(node);
    }

    /**
     * Removes a value subscription from the designated node. The remote
     * endpoint will no longer receive updates when the value updates.
     *
     * @param node Subscribed node to unsubscribe
     */
    public synchronized void removeValueSub(Node node) {
        Integer sid = valueSubsNodes.remove(node);
        valueSubsSids.remove(sid);
    }

    /**
     * Adds a path subscription to the designated node. This will allow a node
     * to publish a child update and have it updated to the remote endpoint if
     * it is subscribed.
     *
     * @param node Node to subscribe to
     * @param resp Response to send updates to
     */
    public synchronized void addPathSub(Node node, ListResponse resp) {
        ListResponse prev = pathSubs.put(node, resp);
        if (prev != null) {
            throw new RuntimeException("Node " + node.getPath() + " already subscribed");
        }
    }

    /**
     * Removes the node from being listened to for children updates.
     *
     * @param node Node to unsubscribe to.
     */
    public synchronized void removePathSub(Node node) {
        pathSubs.remove(node);
        Map<String, Node> children = node.getChildren();
        if (children != null) {
            for (Node child : children.values()) {
                ListResponse resp = pathSubs.get(child);
                if (resp != null) {
                    resp.getCloseResponse();
                }
            }
        }
    }

    public synchronized void postChildUpdate(Node parent, Node child, boolean removed) {
        ListResponse resp = pathSubs.get(parent);
        if (resp != null) {
            resp.childUpdate(child, removed);
        }
    }

    public synchronized void postValueUpdate(Node node) {
        Integer sid = valueSubsNodes.get(node);
        if (sid != null) {
            JsonArray updates = new JsonArray();
            {
                JsonArray update = new JsonArray();
                update.addNumber(sid);
                ValueUtils.toJson(update, node.getValue());
                update.addString(FORMAT.format(new Date()));

                updates.addArray(update);
            }

            JsonObject resp = new JsonObject();
            resp.putNumber("rid", 0);
            resp.putArray("updates", updates);

            JsonArray responses = new JsonArray();
            responses.addObject(resp);
            JsonObject top = new JsonObject();
            top.putArray("responses", responses);
            link.getClient().write(top);
        }
    }
}
