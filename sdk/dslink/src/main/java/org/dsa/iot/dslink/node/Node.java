package org.dsa.iot.dslink.node;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.dslink.node.exceptions.DuplicateException;
import org.dsa.iot.dslink.node.value.Value;
import org.vertx.java.core.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dsa.iot.dslink.node.NodeManager.NodeBooleanTuple;

/**
 * @author Samuel Grenier
 */
@Getter
public class Node {

    private final SubscriptionManager manager;
    private final Node parent;

    private Map<String, Node> children;
    private Map<String, Value> attributes;
    private Map<String, Value> configurations;
    private List<String> interfaces;

    private final String name;
    private final String path;
    private String displayName;
    private Value value;

    @Setter
    private boolean invokable;

    @Setter
    private Handler<Void> invocationHandler;

    private final List<Handler<NodeBooleanTuple>> childrenUpdates = new ArrayList<>();

    /**
     * Whether the node is currently subscribed to or not
     */
    @Setter
    private boolean subscribed;

    /**
     *
     * @param parent The parent of this node, or null if a root node
     * @param name The name of this node
     */
    public Node(SubscriptionManager manager,
                                        Node parent, @NonNull String name) {
        StringUtils.checkNodeName(name);
        this.manager = manager;
        this.parent = parent;
        this.name = name;
        path = parent == null ? "/" + name : parent.getPath() + "/" + name;
        setConfiguration("is", new Value((String) null)); // TODO: full profile support
    }

    public synchronized void setDisplayName(String name) {
        if (name == null) {
            displayName = null;
            return;
        }
        StringUtils.checkNodeName(name);
        this.displayName = name;
    }

    public void setValue(Value value) {
        setValue(value, true);
    }

    public synchronized void setValue(Value value, boolean update) {
        this.value = value;
        if (update)
            update();
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
        return addChild(new Node(manager, this, name));
    }

    public synchronized Node addChild(@NonNull Node node) {
        if (children == null)
            children = new HashMap<>();
        else if (children.containsKey(node.name))
            throw new DuplicateException(node.name + "(parent: " + name + ")");
        children.put(node.name, node);
        notifyChildrenHandlers(node, false);
        return node;
    }

    public Node removeChild(Node node) {
        return removeChild(node.name);
    }

    public synchronized Node removeChild(@NonNull String name) {
        Node n = children != null ? children.remove(name) : null;
        if (n != null)
            notifyChildrenHandlers(n, true);
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

    public synchronized Handler<NodeBooleanTuple> addChildrenHandler(
                                @NonNull Handler<NodeBooleanTuple> handler) {
        childrenUpdates.add(handler);
        return handler;
    }

    public synchronized void removeChildrenHandler(
                                @NonNull Handler<NodeBooleanTuple> handler) {
        childrenUpdates.remove(handler);
    }

    private synchronized void notifyChildrenHandlers(@NonNull Node n,
                                                     boolean removed) {
        for (Handler<NodeBooleanTuple> handler : childrenUpdates) {
            handler.handle(new NodeBooleanTuple(n, removed));
        }
    }
}
