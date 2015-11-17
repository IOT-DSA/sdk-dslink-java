package org.dsa.iot.dslink.node;

import io.netty.util.CharsetUtil;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.FileUtils;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Handles subscriptions for values and paths.
 * @author Samuel Grenier
 */
public class SubscriptionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionManager.class);
    private final Map<String, ListResponse> pathSubsMap = new ConcurrentHashMap<>();

    private final Map<String, Subscription> valueSubsPaths = new HashMap<>();
    private final Map<Integer, String> valueSubsSids = new HashMap<>();
    private final Object valueLock = new Object();

    private final File storageDir = new File("storage");
    private final DSLink link;

    public SubscriptionManager(DSLink link) {
        this.link = link;
        readStorage();
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
            if (prev != null) {
                valueSubsSids.remove(prev.sid());

                JsonArray updates = prev.generateUpdates();
                if (updates != null) {
                    JsonObject resp = new JsonObject();
                    resp.put("rid", 0);
                    resp.put("updates", updates);
                    link.getWriter().writeResponse(resp);
                }
            }
            valueSubsSids.put(sid, path);
            if (prev != null) {
                return;
            }
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
                        sub.addValue(val);
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

    protected void readStorage() {
        if (!storageDir.isDirectory()) {
            return;
        }
        File[] files = storageDir.listFiles();
        if (files == null) {
            return;
        }
        synchronized (valueLock) {
            for (File f : files) {
                if (f == null || !"%2F".startsWith(f.getName())) {
                    continue;
                }
                try {
                    String s = new String(FileUtils.readAllBytes(f), CharsetUtil.UTF_8);
                    JsonObject obj = new JsonObject(s);

                    int qos = obj.get("qos");
                    String path = StringUtils.decodeName(f.getName());
                    Subscription sub = new Subscription(path, -1, qos);
                    valueSubsPaths.put(path, sub);
                    if (qos == 2) {
                        String ts = obj.get("ts");
                        Value val = ValueUtils.toValue(obj.get("value"), ts);
                        sub.addValue(val);
                    } else if (qos == 3) {
                        JsonArray queue = obj.get("queue");
                        if (queue == null) {
                            continue;
                        }
                        for (Object o : queue) {
                            JsonArray array = (JsonArray) o;
                            String ts = array.get(1);
                            Value v = ValueUtils.toValue(array.get(0), ts);
                            sub.addValue(v);
                        }
                    }
                } catch (Exception e) {
                    String path = f.getName();
                    String err = "Failed to handle QoS subscription data: {}\n{}";
                    LOGGER.warn(err, path, e);
                }
            }
        }
    }

    protected class Subscription {

        private final String path;
        private final int sid;
        private final int qos;

        private Queue<Value> cache;
        private Value lastValue;

        public Subscription(String path, int sid, int qos) {
            this.path = path;
            this.sid = sid;
            this.qos = qos;
            if (qos == 1 || qos == 3) {
                cache = new LinkedBlockingQueue<>();
            }
        }

        public int sid() {
            return sid;
        }

        public int qos() {
            return qos;
        }

        public synchronized void addValue(Value value) {
            JsonObject obj = null;
            if (qos() == 2) {
                lastValue = value;
                if (!storageDir.exists() && storageDir.mkdir()) {
                    String full = storageDir.getAbsolutePath();
                    LOGGER.info("Created storage directory at {}", full);
                }
                obj = new JsonObject();
                obj.put("qos", 2);
                if (value != null) {
                    obj.put("value", value);
                    obj.put("ts", value.getTimeStamp());
                }
            } else if (qos() == 3) {
                cache.add(value);
                if (cache.size() > 1000) {
                    cache.remove();
                }
                obj = new JsonObject();
                JsonArray queue = new JsonArray();
                obj.put("queue", queue);
                obj.put("qos", 3);
                for (Value v : cache) {
                    if (v == null) {
                        queue.add(null);
                    } else {
                        JsonArray array = new JsonArray();
                        array.add(v);
                        array.add(v.getTimeStamp());
                        queue.add(array);
                    }
                }
            }
            if (obj != null) {
                File f = new File(storageDir, StringUtils.encodeName(path));
                try {
                    FileUtils.write(f, obj.encode().getBytes(CharsetUtil.UTF_8));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public synchronized JsonArray generateUpdates() {
            {
                Value tmp = lastValue;
                if (lastValue != null) {
                    JsonArray update = generateUpdate(tmp);
                    lastValue = null;
                    return update;
                }
                if (cache == null || cache.isEmpty()) {
                    return null;
                }
            }
            JsonArray updates = new JsonArray();
            Iterator<Value> it = cache.iterator();
            while (it.hasNext()) {
                JsonArray update = generateUpdate(it.next());
                updates.add(update);
                it.remove();
            }
            {
                File f = new File(storageDir, StringUtils.encodeName(path));
                if (f.exists()) {
                    if (!f.delete()) {
                        LOGGER.warn("Failed to delete QoS data at {}", path);
                    }
                }
            }
            return updates;
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
