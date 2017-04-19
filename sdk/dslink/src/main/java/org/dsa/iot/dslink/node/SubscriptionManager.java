package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.storage.FileDriver;
import org.dsa.iot.dslink.node.storage.StorageDriver;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Handles subscriptions for values and paths.
 * @author Samuel Grenier
 */
public class SubscriptionManager {

    private static final StorageDriver DRIVER = new FileDriver();

    private final Map<String, ListResponse> pathSubsMap = new ConcurrentHashMap<>();
    private final Map<String, Subscription> valueSubsPaths = new HashMap<>();
    private final Map<Integer, String> valueSubsSids = new HashMap<>();
    private final Object valueLock = new Object();
    private final DSLink link;

    public SubscriptionManager(DSLink link) {
        this.link = link;
        synchronized (valueLock) {
            DRIVER.read(valueSubsPaths);
        }
    }

    public void disconnected() {
        ScheduledThreadPoolExecutor stpe = Objects.getDaemonThreadPool();
        NodeManager manager = link.getNodeManager();

        Map<String, Subscription> subs;
        synchronized (valueLock) {
            subs = new HashMap<>(valueSubsPaths);
            valueSubsSids.clear();
        }

        {
            Iterator<Map.Entry<String, Subscription>> it = subs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Subscription> entry = it.next();
                Subscription sub = entry.getValue();
                if (sub.qos() > 0) {
                    continue;
                }
                it.remove();
                String path = entry.getKey();
                Node node = manager.getNode(path, false, false).getNode();
                if (node == null) {
                    continue;
                }
                final NodeListener listener = node.getListener();
                if (listener != null) {
                    stpe.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.postOnUnsubscription();
                        }
                    });
                }
            }
        }

        {
            Iterator<String> it = pathSubsMap.keySet().iterator();
            while (it.hasNext()) {
                String path = it.next();
                it.remove();
                Node node = manager.getNode(path, false, false).getNode();
                if (node == null) {
                    continue;
                }
                final NodeListener listener = node.getListener();
                if (listener != null) {
                    stpe.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.postListClosed();
                        }
                    });
                }
            }
        }
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
        return node != null && pathSubsMap.containsKey(node.getPath());
    }

    /**
     * Adds a value subscription to the designated node. This will allow a node
     * to publish a value update and have it updated to the remote endpoint if
     * it is subscribed.
     *
     * @param path Path to subscribe to.
     * @param sid Subscription ID to send back to the client.
     * @param qos The QoS level of the designated subscription.
     */
    public void addValueSub(String path, int sid, int qos) {
        path = NodeManager.normalizePath(path, true);
        synchronized (valueLock) {
            Subscription sub = new Subscription(path, sid, qos);
            Subscription prev = valueSubsPaths.put(path, sub);
            boolean ret = false;
            if (prev != null) {
                ret = true;
                valueSubsSids.remove(prev.sid());
                JsonArray updates = DRIVER.getUpdates(sub);
                if (updates != null) {
                    JsonObject resp = new JsonObject();
                    resp.put("rid", 0);
                    resp.put("updates", updates);
                    link.getWriter().writeResponse(resp);
                    ret = true;
                }
            }
            valueSubsSids.put(sid, path);
            if (ret) {
                return;
            }
        }
        NodeManager man = link.getNodeManager();
        Node node = man.getNode(path, false, false).getNode();
        if (node != null) {
        	if (node.shouldPostCachedValue()) {
        		postValueUpdate(node);
        	}
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
            if (path == null) {
                return;
            }
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
        Subscription sub;
        synchronized (valueLock) {
            sub = valueSubsPaths.remove(node.getPath());
        }
        if (sub != null) {
            removeValueSub(sub.sid());
        }
    }

    /**
     * Adds a path subscription to the designated node. This will allow a node
     * to publish a child update and have it updated to the remote endpoint if
     * it is subscribed.
     *
     * @param path Path to subscribe to
     * @param resp Response to send updates to
     */
    public void addPathSub(String path, ListResponse resp) {
        if (path == null) {
            return;
        }
        pathSubsMap.put(path, resp);
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
        pathSubsMap.remove(node.getPath());
    }

    /**
     * Posts a child update to notify all remote endpoints of an update.
     *
     * @param child Updated child.
     * @param removed Whether the child was removed or not.
     */
    public void postChildUpdate(Node child, boolean removed) {
        Node parent = child.getParent();
        if (parent == null) {
            return;
        }
        ListResponse resp = pathSubsMap.get(parent.getPath());
        if (resp != null) {
            resp.childUpdate(child, removed);
        }
    }

    /**
     * Posts multiple children updates to notify all remote endpoints of updates.
     *
     * @param parent Common parent.
     * @param children Children.
     */
    public void postMultiChildUpdate(Node parent, List<Node> children) {
        if (parent == null) {
            return;
        }
        ListResponse resp = pathSubsMap.get(parent.getPath());
        if (resp != null) {
            resp.multiChildrenUpdate(children);
        }
    }

    public void batchValueUpdate(Map<Node, Value> updates) {
        batchValueUpdate(updates, true);
    }

    public void batchValueUpdate(Map<Node, Value> updates,
                                 boolean set) {
        if (updates == null) {
            return;
        }
        JsonArray jsonUpdates = null;
        for (Map.Entry<Node, Value> entry : updates.entrySet()) {
            Node node = entry.getKey();
            Value val = entry.getValue();
            if (set) {
                node.setValue(val, false, false);
            }

            Subscription sub = valueSubsPaths.get(node.getPath());
            if (sub != null) {
                if (!link.isConnected()) {
                    if (sub.qos() > 0) {
                        DRIVER.store(sub, val);
                    }
                    continue;
                }
                if (jsonUpdates == null) {
                    jsonUpdates = new JsonArray();
                }
                jsonUpdates.add(sub.generateUpdate(val));
            }
        }

        if (jsonUpdates != null) {
            JsonObject resp = new JsonObject();
            resp.put("rid", 0);
            resp.put("updates", jsonUpdates);
            link.getWriter().writeResponse(resp);
        }
    }

    /**
     * Posts a value update to notify all the remote endpoints of a node
     * value update.
     *
     * @param node Updated node.
     */
    public void postValueUpdate(Node node) {
        Value value = node.getValue();
        Map<Node, Value> map = Collections.singletonMap(node, value);
        batchValueUpdate(map, false);
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
        ListResponse resp = pathSubsMap.get(node.getPath());
        if (resp != null) {
            resp.metaUpdate(name, value);
        }
    }

    public static class Subscription {

        private final String path;
        private final int sid;
        private final int qos;

        public Subscription(String path, int sid, int qos) {
            this.path = StringUtils.encodeName(path);
            this.sid = sid;
            this.qos = qos;
        }

        public int sid() {
            return sid;
        }

        public int qos() {
            return qos;
        }

        public String path() {
            return path;
        }

        public JsonArray generateUpdate(Value val) {
            JsonArray update = new JsonArray();
            update.add(sid());

            if (val != null) {
                update.add(val);
                update.add(val.getTimeStamp());
            } else {
                update.add(null);
            }
            return update;
        }
    }
}
