package org.dsa.iot.dslink.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.dslink.events.ChildrenUpdateEvent;
import org.dsa.iot.dslink.events.ClosedStreamEvent;
import org.dsa.iot.dslink.node.exceptions.DuplicateException;
import org.dsa.iot.dslink.node.value.Value;
import org.vertx.java.core.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Node {

    private final SubscriptionManager manager;
    private final Node parent;

    private Map<String, Node> children;
    private Map<String, Value> attributes;
    private Map<String, Value> configurations;
    private List<String> interfaces;

    private final EventBus bus;

    @Getter
    private final String name;

    @Getter
    private final String path;

    private String displayName;
    private Value value;

    @Getter
    @Setter
    private boolean invokable;

    @Getter
    @Setter
    private Handler<Void> invocationHandler;

    /**
     * Whether the node is currently subscribed to or not
     */
    @Getter
    @Setter
    private boolean subscribed;

    /**
     * Request ID to send subscription updates on, or -1 for none
     */
    @Getter
    @Setter
    private int childrenRid = -1;

    /**
     * @param bus Event bus to publish events to
     * @param parent The parent of this node, or null if a root node
     * @param name The name of this node
     */
    public Node(EventBus bus, SubscriptionManager manager,
                                        Node parent, @NonNull String name) {
        StringUtils.checkNodeName(name);
        this.bus = bus;
        this.manager = manager;
        this.parent = parent;
        this.name = name;
        path = parent == null ? "/" + name : parent.getPath() + "/" + name;
        setConfiguration("is", new Value((String) null)); // TODO: full profile support
    }

    protected void init() {
        bus.register(this);
    }

    protected void deInit() {
        bus.unregister(this);
    }

    public synchronized String getDisplayName() {
        return displayName;
    }

    public synchronized Value getValue() {
        return value;
    }

    public synchronized List<String> getInterfaces() {
        return interfaces != null ? ImmutableList.copyOf(interfaces) : null;
    }

    public synchronized Map<String, Node> getChildren() {
        return children != null ? ImmutableMap.copyOf(children) : null;
    }

    public synchronized Map<String, Value> getAttributes() {
        return attributes != null ? ImmutableMap.copyOf(attributes) : null;
    }

    public synchronized Map<String, Value> getConfigurations() {
        return configurations != null ? ImmutableMap.copyOf(configurations) : null;
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
        if (!(value == null || this.value == null || value.equals(this.value))) {
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

        if (attributes.containsKey(name))
            throw new DuplicateException(name);
        else if (value == null)
            attributes.remove(name);
        else
            attributes.put(name, value);
    }

    public synchronized void setConfiguration(@NonNull String name, Value value) {
        StringUtils.checkNodeName(name);
        if (configurations == null)
            configurations = new HashMap<>();

        if (configurations.containsKey(name))
            throw new DuplicateException(name);
        else if (value == null)
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

    public Node createChild(String name) {
        return addChild(new Node(bus, manager, this, name));
    }

    public synchronized Node addChild(@NonNull Node node) {
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

    public synchronized void clearInterfaces() {
        if (interfaces != null)
            interfaces.clear();
    }

    public synchronized void removeInterface(@NonNull String name) {
        if (interfaces != null)
            interfaces.remove(name);
    }

    public synchronized Value removeAttribute(@NonNull String name) {
        return attributes != null ? attributes.remove(name) : null;
    }

    public synchronized Value removeConfiguration(@NonNull String name) {
        return configurations != null ? configurations.remove(name) : null;
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

    private void update() {
        if (manager != null && isSubscribed()) {
            manager.update(this);
        }
    }

    private synchronized void notifyChildrenHandlers(@NonNull Node n,
                                                     boolean removed) {
        if (childrenRid != -1) {
            bus.post(new ChildrenUpdateEvent(n, removed, childrenRid));
        }
    }

    @Subscribe
    public void closedStream(ClosedStreamEvent event) {
        if (event.getRid() == childrenRid) {
            childrenRid = -1;
        }
    }
}
