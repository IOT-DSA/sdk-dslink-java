package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles subscriptions for values and paths.
 * @author Samuel Grenier
 */
public class SubscriptionManager {

    private final Object valueLock = new Object();

    private final Map<Node, ListResponse> pathSubsMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> pathSubs = new ConcurrentHashMap<>();

    private final Map<String, Integer> valueSubsPaths = new ConcurrentHashMap<>();
    private final Map<Integer, String> valueSubsSids = new ConcurrentHashMap<>();
    private final DSLink link;

    public SubscriptionManager(DSLink link) {
        this.link = link;
    }

    /**
     * Tests whether the node has a value subscription that a remote endpoint
     * is listening to.
     *
     * @param node Node to test.
     * @return Whether the node has a value subscription.
     */
    @SuppressWarnings("unused")
    public boolean hasValueSub(Node node) {
        return valueSubsPaths.containsKey(node.getPath());
    }

    /**
     * Tests whether the node has a path subscription that a remote endpoint
     * is listening to.
     *
     * @param node Node to test.
     * @return Whether the node has a path subscription.
     */
    @SuppressWarnings("unused")
    public boolean hasPathSub(Node node) {
        return pathSubsMap.containsKey(node);
    }

    /**
     * Adds a value subscription to the designated node. This will allow a node
     * to publish a value update and have it updated to the remote endpoint if
     * it is subscribed.
     *
     * @param path Path to subscribe to
     * @param sid Subscription ID to send back to the client
     */
    public void addValueSub(String path, int sid) {
        path = NodeManager.normalizePath(path, true);
        synchronized (valueLock) {
            valueSubsPaths.put(path, sid);
            valueSubsSids.put(sid, path);
        }
        NodeManager man = link.getNodeManager();
        Node node = man.getNode(path, false, false).getNode();
        if (node != null) {
            postValueUpdate(node);
            node.getListener().postOnSubscription();
        }
    }

    /**
     * Removes a value subscription from the designated node. The remote
     * endpoint will no longer receive updates when the value updates.
     *
     * @param sid Subscription ID to unsubscribe
     */
    public void removeValueSub(int sid) {
        String path;
        synchronized (valueLock) {
            path = valueSubsSids.remove(sid);
            valueSubsPaths.remove(path);
        }

        Node node = null;
        {
            NodeManager man = link.getNodeManager();
            if (man != null) {
                node = man.getNode(path, false, false).getNode();
            }
        }
        if (node != null) {
            node.getListener().postOnUnsubscription();
        }
    }

    /**
     * Removes a value subscription from the designated node. The remote
     * endpoint will no longer receive updates when the value updates.
     *
     * @param node Subscribed node to unsubscribe
     */
    public void removeValueSub(Node node) {
        Integer sid;
        synchronized (valueLock) {
            sid = valueSubsPaths.remove(node.getPath());
            if (sid != null) {
                valueSubsSids.remove(sid);
            }
        }
        if (sid != null) {
            node.getListener().postOnUnsubscription();
        }
    }

    /**
     * Adds a path subscription to the designated node. This will allow a node
     * to publish a child update and have it updated to the remote endpoint if
     * it is subscribed.
     *
     * @param node Node to subscribe to
     * @param resp Response to send updates to
     */
    public void addPathSub(Node node, ListResponse resp) {
        pathSubsMap.put(node, resp);
    }

    /**
     * Removes the node from being listened to for children updates.
     *
     * @param node Node to unsubscribe to.
     */
    public void removePathSub(Node node) {
        if (node == null) {
            return;
        }
        ListResponse resp = pathSubsMap.remove(node);
        if (resp != null) {
            Map<String, Node> children = node.getChildren();
            if (children != null) {
                for (Node child : children.values()) {
                    resp = pathSubsMap.get(child);
                    if (resp != null) {
                        resp.getCloseResponse();
                    }
                }
            }
        }
    }

    /**
     * Posts a child update to notify all remote endpoints of an update.
     *
     * @param child Updated child.
     * @param removed Whether the child was removed or not.
     */
    public void postChildUpdate(Node child, boolean removed) {
        ListResponse resp = null;
        Integer i = pathSubs.remove(child.getPath());
        if (i != null) {
            Node parent = child.getParent();
            resp = new ListResponse(link, this, 0, parent);
            addPathSub(parent, resp);
        }

        if (resp == null) {
            resp = pathSubsMap.get(child.getParent());
        }

        if (resp != null) {
            resp.childUpdate(child, removed);
        }
    }

    /**
     * Posts a value update to notify all the remote endpoints of a node
     * value update.
     *
     * @param node Updated node.
     */
    public void postValueUpdate(Node node) {
        Integer sid = valueSubsPaths.get(node.getPath());
        if (sid != null) {
            JsonArray updates = new JsonArray();
            {
                JsonArray update = new JsonArray();
                update.addNumber(sid);

                Value value = node.getValue();
                if (value != null) {
                    ValueUtils.toJson(update, value);
                    update.addString(value.getTimeStamp());
                } else {
                    update.add(null);
                }

                updates.addArray(update);
            }

            JsonObject resp = new JsonObject();
            resp.putNumber("rid", 0);
            resp.putArray("updates", updates);
            link.getWriter().writeResponse(resp);
        }
    }

    /**
     * Updates the internal data of a node such as a configuration or an
     * attribute.
     *
     * @param node Updated node.
     * @param name The name of what is being updated.
     * @param value The new value of the update.
     */
    public void postMetaUpdate(Node node, String name, Value value) {
        ListResponse resp = pathSubsMap.get(node);
        if (resp != null) {
            resp.metaUpdate(name, value);
        }
    }
}
