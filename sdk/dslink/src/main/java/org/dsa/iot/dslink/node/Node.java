package org.dsa.iot.dslink.node;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import net.engio.mbassy.bus.MBassador;

import org.dsa.iot.core.StringUtils;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.events.ClosedStreamEvent;
import org.dsa.iot.dslink.node.exceptions.DuplicateException;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.responder.Responder;
import org.dsa.iot.dslink.responder.action.Action;
import org.dsa.iot.dslink.responder.methods.ListMethod;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class Node {
    @Getter
    private final WeakReference<Node> parent;

    private Map<String, Node> children;
    private Map<String, Value> attributes;
    private Map<String, Value> configurations;
    private List<String> interfaces;
    private Map<Client, Integer> childrenSubs;
    private Map<Client, Subscription> subs;

    private final MBassador<Event> bus;

    @Getter
    private final String name;

    @Getter
    private final String path;

    private String displayName;
    private Value value;

    @Getter
    @Setter
    private Action action;

    /**
     * @param bus
     *            Event bus to publish events to
     * @param parent
     *            The parent of this node, or null if a root node
     * @param name
     *            The name of this node
     */
    public Node(MBassador<Event> bus, Node parent, @NonNull String name) {
        this.bus = bus;
        this.parent = new WeakReference<>(parent);
        this.name = name;
        this.childrenSubs = new WeakHashMap<>();
        if (isRootNode()) {
            path = name;
        } else {
            StringUtils.checkNodeName(name);
            path = parent == null ? "/" + name : parent.getPath() + "/" + name;
        }
        setConfiguration("is", new Value("node")); // TODO: full profile support
    }

    protected void init() {
        if (bus != null) {
            bus.subscribe(this);
        }
    }

    protected void deInit() {
        if (bus != null) {
            bus.unsubscribe(this);
        }
    }

    public synchronized String getDisplayName() {
        return displayName;
    }

    public synchronized Value getValue() {
        return value;
    }

    public synchronized List<String> getInterfaces() {
        return interfaces != null ? new ArrayList<>(interfaces) : null;
    }

    public synchronized Map<String, Node> getChildren() {
        return children != null ? new HashMap<>(children) : null;
    }

    public synchronized Map<String, Value> getAttributes() {
        return attributes != null ? new HashMap<>(attributes) : null;
    }

    public synchronized Map<String, Value> getConfigurations() {
        val copy = configurations != null ? new HashMap<>(configurations) : null;
        if (copy != null) {
            if (getDisplayName() != null) {
                copy.put("$name", new Value(getDisplayName()));
            }
        }
        return copy;
    }

    public synchronized void setDisplayName(String name) {
        if (name == null) {
            displayName = null;
            return;
        }
        StringUtils.checkNodeName(name);
        this.displayName = name;
    }

    public synchronized void setValue(Value value) {
        setValue(value, true);
    }

    public synchronized void setValue(Value value, boolean update) {
        if (!(value == null || value.equals(this.value))) {
            this.value = value;
            this.value.setImmutable();
            if (update)
                update();
        }
    }

    public synchronized void setAttribute(@NonNull String name, Value value) {
        StringUtils.checkNodeName(name);
        if (attributes == null)
            attributes = new HashMap<>();
        else if (attributes.containsKey(name))
            attributes.remove(name);

        if (value == null)
            attributes.remove(name);
        else
            attributes.put(name, value);
    }

    public synchronized void setConfiguration(@NonNull String name, Value value) {
        StringUtils.checkNodeName(name);
        if (configurations == null)
            configurations = new HashMap<>();
        else if (configurations.containsKey(name))
            configurations.remove(name);

        if (value == null)
            configurations.remove(name);
        else
            configurations.put(name, value);
    }

    public synchronized void addInterface(@NonNull String name) {
        if (interfaces == null)
            interfaces = new ArrayList<>();
        if (!interfaces.contains(name))
            interfaces.add(name);
    }

    public Node createChild(String name) throws DuplicateException {
        return addChild(new Node(bus, this, name));
    }

    public synchronized Node addChild(@NonNull Node node)
                                                    throws DuplicateException {
        if (children == null)
            children = new HashMap<>();
        else if (children.containsKey(node.name))
            throw new DuplicateException(node.name + "(parent: " + name + ")");
        node.init();
        children.put(node.name, node);
        notifyChildrenHandlers(node, false);
        return node;
    }

    public Node removeChild(Node node) {
        return removeChild(node.name);
    }

    public synchronized Node removeChild(@NonNull String name) {
        Node n = children != null ? children.remove(name) : null;
        if (n != null) {
            n.deInit();
            notifyChildrenHandlers(n, true);
        }
        return n;
    }

    public synchronized void clearChildren() {
        if (children == null)
            return;

        val it = children.values().iterator();
        while (it.hasNext()) {
            val node = it.next();
            node.deInit();
            notifyChildrenHandlers(node, true);
            it.remove();
        }
    }

    public synchronized void clearInterfaces() {
        if (interfaces != null)
            interfaces.clear();
    }

    public synchronized void removeInterface(@NonNull String name) {
        if (interfaces != null)
            interfaces.remove(name);
    }

    public synchronized Value getAttribute(@NonNull String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    public synchronized Value getConfiguration(@NonNull String name) {
        return configurations != null ? configurations.get(name) : null;
    }

    public synchronized Node getChild(@NonNull String name) {
        return children != null ? children.get(name) : null;
    }

    private synchronized void update() {
        if (subs != null) {
            for (Subscription sub : subs.values()) {
                sub.update(this);
            }
        }
    }

    public synchronized void subscribe(@NonNull Subscription sub)
                                            throws DuplicateException {
        val client = sub.getClient();
        if (subs == null)
            subs = new HashMap<>();
        else if (subs.containsKey(client))
            throw new DuplicateException("Client already subscribed");
        subs.put(client, sub);
    }

    public synchronized void unsubscribe(@NonNull Client client) {
        subs.remove(client);
    }

    public synchronized void subscribeToChildren(@NonNull Client client,
                                        @NonNull Responder responder, int rid) {
        unsubscribeFromChildren(client, responder);
        childrenSubs.put(client, rid);
    }

    public synchronized void unsubscribeFromChildren(@NonNull Client client,
                                                @NonNull Responder responder) {
        Integer rid = childrenSubs.remove(client);
        if (rid != null) {
            responder.closeStream(client, rid);
        }
    }

    private synchronized void notifyChildrenHandlers(@NonNull Node n,
                                                        boolean removed) {
        val iterator = childrenSubs.entrySet().iterator();
        while (iterator.hasNext()) {
            val entry = iterator.next();
            val client = entry.getKey();
            val tracker = client.getResponseTracker();
            val rid = entry.getValue();
            if (tracker.isTracking(rid)) {
                val response = new JsonObject();
                response.putNumber("rid", rid);
                response.putString("stream", StreamState.OPEN.jsonName);

                val updates = new JsonArray();
                updates.addElement(ListMethod.getChildUpdate(n, removed));
                response.putArray("updates", updates);
                if (!client.write(response)) {
                    iterator.remove();
                }
            } else {
                // Ensure the object is cleaned up
                iterator.remove();
            }
        }
    }

    @net.engio.mbassy.listener.Handler
    public synchronized void closedStream(ClosedStreamEvent event) {
        val client = event.getClient();
        Integer rid = childrenSubs.get(client);
        if (rid != null && rid == event.getRid()) {
            unsubscribeFromChildren(client, event.getResponder());
        }
    }

    protected boolean isRootNode() {
        return false;
    }
}
