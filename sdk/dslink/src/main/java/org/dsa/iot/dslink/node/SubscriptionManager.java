package org.dsa.iot.dslink.node;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.internal.SystemPropertyUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.PropertyReference;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles subscriptions for values and paths.
 *
 * @author Samuel Grenier
 */
public class SubscriptionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDriver.class);
    public static final int QOS_QUEUE_SIZE;

    private boolean connected = false;
    private DSLink link;
    private Map<String, ListResponse> pathSubsMap = new ConcurrentHashMap<>();
    private FileDriver storage;
    private SubscriptionWriter subscriptionWriter;
    private Object valueLock = new Object();
    private Map<String, Subscription> valueSubsPaths = new HashMap<>();
    private Map<Integer, String> valueSubsSids = new HashMap<>();

    public SubscriptionManager(DSLink link) {
        this.link = link;
        if (link.isResponder()) {
            subscriptionWriter = new SubscriptionWriter();
            storage = new FileDriver(this);
            storage.restore();
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
     * Adds a value subscription to the designated node. This will allow a node
     * to publish a value update and have it updated to the remote endpoint if
     * it is subscribed.
     *
     * @param path Path to subscribe to.
     * @param sid  Subscription ID to send back to the client.
     * @param qos  The QoS level of the designated subscription.
     */
    public void addValueSub(String path, int sid, int qos) {
        path = NodeManager.normalizePath(path, true);
        boolean clearStorage = false;
        Subscription sub;
        synchronized (valueLock) {
            sub = valueSubsPaths.get(path);
            if (sub != null) {
                if (sub.getSid() < 0) {
                    clearStorage = true;
                } else {
                    valueSubsSids.remove(sub.getSid());
                }
                sub.update(sid, qos);
            } else {
                sub = new Subscription(path, sid, qos);
                valueSubsPaths.put(path, sub);
            }
            valueSubsSids.put(sid, path);
        }
        if (clearStorage) {
            storage.clear(sub);
        }
        if (sub.hasUpdates()) {
            sub.enqueue();
        } else {
            NodeManager man = link.getNodeManager();
            Node node = man.getNode(path, false, false).getNode();
            if (node != null) {
                if (node.shouldPostCachedValue()) {
                    postValueUpdate(node);
                }
                node.getListener().postOnSubscription();
            }
        }
    }

    public void batchValueUpdate(Map<Node, Value> updates, boolean set) {
        if (updates == null) {
            return;
        }
        Subscription sub;
        for (Map.Entry<Node, Value> entry : updates.entrySet()) {
            Node node = entry.getKey();
            Value val = entry.getValue();
            if (set) {
                node.setValue(val, false, false);
            }
            synchronized (valueLock) {
                sub = valueSubsPaths.get(node.getPath());
            }
            if (sub != null) {
                sub.postUpdate(val);
            }
        }
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
        synchronized (valueLock) {
            return node != null && pathSubsMap.containsKey(node.getPath());
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
        synchronized (valueLock) {
            return valueSubsPaths.containsKey(node.getPath());
        }
    }

    /**
     * Called when the link is connected to an upstream broker;
     */
    public void onConnected() {
        connected = true;
        if (subscriptionWriter != null) {
            synchronized (subscriptionWriter) {
                subscriptionWriter.notifyAll();
            }
        }
    }

    /**
     * Called when the link is disconnected from it's upstream broker;
     */
    public void onDisconnected() {
        connected = false;
        if (subscriptionWriter != null) {
            subscriptionWriter.clearQueue();
        }
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
                sub.onDisconnected();
                if (sub.getQos() > 1) {
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
            Responder responder = link.getResponder();
            Iterator<Map.Entry<String, ListResponse>> it = pathSubsMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ListResponse> entry = it.next();
                it.remove();
                if (entry.getValue() != null) {
                    responder.removeResponse(entry.getValue().getRid());
                }
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
                            listener.postListClosed();
                        }
                    });
                }
            }
        }
    }

    /**
     * Posts a child update to notify all remote endpoints of an update.
     *
     * @param child   Updated child.
     * @param removed Whether the child was removed or not.
     */
    public void postChildUpdate(Node child, boolean removed) {
        Node parent = child.getParent();
        if (parent == null) {
            return;
        }
        if (!link.isConnected()) {
            return;
        }
        ListResponse resp = pathSubsMap.get(parent.getPath());
        if (resp != null) {
            resp.childUpdate(child, removed);
        }
        if (removed) {
            nodeRemoved(child);
        } else {
            nodeAdded(child);
        }
    }

    /**
     * Updates the internal data of a node such as a configuration or an
     * attribute.
     *
     * @param node  Updated node.
     * @param name  The name of what is being updated.
     * @param value The new value of the update.
     */
    public void postMetaUpdate(Node node, String name, Value value) {
        if (!link.isConnected()) {
            return;
        }
        ListResponse resp = pathSubsMap.get(node.getPath());
        if (resp != null) {
            resp.metaUpdate(name, value);
        }
    }

    /**
     * Posts multiple children updates to notify all remote endpoints of updates.
     *
     * @param parent   Common parent.
     * @param children Children.
     */
    public void postMultiChildUpdate(Node parent, List<Node> children) {
        if (parent == null) {
            return;
        }
        if (!link.isConnected()) {
            return;
        }
        ListResponse resp = pathSubsMap.get(parent.getPath());
        if (resp != null) {
            resp.multiChildrenUpdate(children);
        }
    }

    /**
     * Posts a value update to notify all the remote endpoints of a node
     * value update.
     *
     * @param node Updated node.
     */
    public void postValueUpdate(Node node) {
        Subscription sub;
        synchronized (valueLock) {
            sub = valueSubsPaths.get(node.getPath());
        }
        if (sub != null) {
            sub.postUpdate(node.getValue());
        }
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
            removeValueSub(sub.getSid());
        }
    }

    /**
     * FileDriver uses this to restore a subscription at startup.
     */
    void restore(String path, Queue<Value> updates) {
        Subscription sub = new Subscription(path, -1, 3);
        sub.updates = updates;
        synchronized (valueLock) {
            valueSubsPaths.put(path, sub);
        }
    }

    private void nodeAdded(Node node) {
        ListResponse list = pathSubsMap.get(node.getPath());
        if (list != null) {
            list.nodeAdded(node);
        }
        if ((node.getHasChildren() != null) && node.getHasChildren()) {
            Iterator<Node> it = node.childIterator();
            while (it.hasNext()) {
                nodeAdded(it.next());
            }
        }
        if (node.getValue() != null) {
            postValueUpdate(node);
        }
    }

    private void nodeRemoved(Node node) {
        ListResponse list = pathSubsMap.get(node.getPath());
        if (list != null) {
            list.nodeRemoved();
        }
        Iterator<Node> it = node.childIterator();
        while (it.hasNext()) {
            nodeRemoved(it.next());
        }
    }

    class Subscription implements MessageGenerator {

        boolean enqueued = false;
        private int lastMid = -1;
        private Value lastUpdate;
        private Queue<Value> lastUpdates;
        private String path;
        private int qos;
        private int sid;
        @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
        private Queue<Value> updates;

        Subscription(String path, int sid, int qos) {
            this.path = StringUtils.encodeName(path);
            this.sid = sid;
            this.qos = qos;
        }

        @Override
        public JsonObject getMessage(int lastAckId) {
            if (qos == 0) {
                Value val;
                synchronized (this) {
                    val = lastUpdate;
                    lastUpdate = null;
                    if (val == null) {
                        return null;
                    }
                }
                JsonArray ary = new JsonArray();
                ary.add(generateUpdate(val));
                JsonObject msg = new JsonObject();
                msg.put("rid", 0);
                msg.put("updates", ary);
                return msg;
            }
            if (lastMid > lastAckId) {
                Thread.yield();
                enqueue();
                return null;
            }
            JsonArray ary;
            synchronized (this) {
                if (updates == null) {
                    return null;
                }
                int size = updates.size();
                if (size == 0) {
                    return null;
                }
                if (size < 1024) { //don't want messages too large
                    lastUpdates = updates;
                    updates = null;
                } else {
                    lastUpdates = new LinkedList<Value>();
                    for (int i = 1024; --i >= 0; ) {
                        lastUpdates.add(updates.poll());
                    }
                }
                ary = new JsonArray();
                for (Value val : lastUpdates) {
                    ary.add(generateUpdate(val));
                }
            }
            JsonObject msg = new JsonObject();
            msg.put("rid", 0);
            msg.put("updates", ary);
            if (qos == 1) {
                synchronized (this) {
                    if (updates == null) {
                        lastUpdates.clear();
                        updates = lastUpdates;
                        lastUpdates = null;
                    }
                }
            }
            if (hasUpdates()) {
                enqueue();
            }
            return msg;
        }

        @Override
        public void retry() {
            enqueue();
        }

        @Override
        public void setMessageId(int lastMessageId) {
            lastMid = lastMessageId;
        }

        private void enqueue() {
            synchronized (SubscriptionManager.this) {
                if (subscriptionWriter == null) {
                    subscriptionWriter = new SubscriptionWriter();
                }
            }
            subscriptionWriter.enqueue(this);
        }

        private JsonArray generateUpdate(Value val) {
            JsonArray update = new JsonArray();
            update.add(getSid());

            if (val != null) {
                update.add(val);
                update.add(val.getTimeStamp());
            } else {
                update.add(null);
            }
            return update;
        }

        String getPath() {
            return path;
        }

        int getQos() {
            return qos;
        }

        int getSid() {
            return sid;
        }

        Queue<Value> getUpdates() {
            return updates;
        }

        synchronized boolean hasUpdates() {
            if ((qos == 0) && (lastUpdate != null)) {
                return true;
            }
            if (updates != null) {
                return !updates.isEmpty();
            }
            return false;
        }

        synchronized void onDisconnected() {
            lastMid = -1;
            sid = -1;
            if (lastUpdates != null) {
                if (updates != null) {
                    lastUpdates.addAll(updates);
                    updates = lastUpdates;
                    lastUpdates = null;
                    if (qos == 3) {
                        storage.store(this);
                    }
                }
            }
        }

        void postUpdate(Value value) {
            if (!connected && (qos < 2)) {
                return;
            }
            synchronized (this) {
                if (qos == 0) {
                    lastUpdate = value;
                } else {
                    if (updates == null) {
                        updates = new LinkedList<Value>();
                    }
                    updates.add(value);
                    if (QOS_QUEUE_SIZE > 0) {
                        while (updates.size() > QOS_QUEUE_SIZE) {
                            updates.remove();
                        }
                    }
                }
                if ((qos == 3) && !connected) {
                    storage.store(this);
                }
            }
            if (sid >= 0) {
                enqueue();
            }
        }

        void update(int sid, int qos) {
            this.sid = sid;
            this.qos = qos;
            synchronized (this) {
                if (qos == 0) {
                    lastUpdates = null;
                    updates = null;
                }
            }
        }

    }

    private class SubscriptionWriter implements Runnable {

        private Queue<Subscription> queue = new LinkedList<Subscription>();

        public SubscriptionWriter() {
            new Thread(this, "Subscription Writer").start();
        }

        /**
         * Returns true if enqueued to be written.
         */
        public synchronized Subscription dequeue() {
            Subscription ret = queue.poll();
            if (ret != null) {
                ret.enqueued = false;
            }
            return ret;
        }

        /**
         * Returns true if enqueued to be written.
         */
        public synchronized boolean enqueue(Subscription sub) {
            if (connected) {
                if (sub.enqueued) {
                    return false;
                }
                sub.enqueued = true;
                queue.add(sub);
            }
            notifyAll();
            return false;
        }

        public void run() {
            Subscription sub = null;
            while (true) {
                synchronized (this) {
                    while (!connected) {
                        try {
                            wait(10000);
                        } catch (Exception ignore) {
                        }
                    }
                    while (connected && queue.isEmpty()) {
                        try {
                            wait(10000);
                        } catch (Exception x) {
                            LOGGER.trace("Ignore", x);
                        }
                    }
                    if (connected) {
                        sub = dequeue();
                    }
                }
                if (sub != null) {
                    try {
                        link.getWriter().writeResponse(sub);
                    } catch (Exception x) {
                        LOGGER.warn(sub.path, x);
                    }
                }
            }
        }

        synchronized void clearQueue() {
            Subscription sub = queue.poll();
            while (sub != null) {
                sub.enqueued = false;
                sub = queue.poll();
            }
        }

    }

    static {
        String s = PropertyReference.QOS_QUEUE_SIZE;
        QOS_QUEUE_SIZE = SystemPropertyUtil.getInt(s, 0);
    }

}
