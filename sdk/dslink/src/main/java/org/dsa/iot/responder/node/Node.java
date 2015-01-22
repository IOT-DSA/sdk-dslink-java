package org.dsa.iot.responder.node;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.responder.node.exceptions.DuplicateException;
import org.dsa.iot.responder.node.value.Value;
import org.vertx.java.core.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dsa.iot.responder.node.NodeManager.NodeBooleanTuple;

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
    }

    public void setDisplayName(String name) {
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

    public void setValue(Value value, boolean update) {
        this.value = value;
        if (update)
            update();
    }

    public void setAttribute(@NonNull String name, Value value) {
        StringUtils.checkNodeName(name);

        if (attributes == null)
            attributes = new HashMap<>();
        else if (attributes.containsKey(name))
            throw new DuplicateException(name);
        else if (value == null)
            attributes.remove(name);
        else
            attributes.put(name, value);
    }

    public void setConfiguration(@NonNull String name, Value value) {
        StringUtils.checkNodeName(name);

        if (configurations == null)
            configurations = new HashMap<>();
        else if (configurations.containsKey(name))
            throw new DuplicateException(name);
        else if (value == null)
            configurations.remove(name);
        else
            configurations.put(name, value);
    }

    public Node createChild(String name) {
        return addChild(new Node(manager, this, name));
    }

    public Node addChild(@NonNull Node node) {
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

    public Node removeChild(@NonNull String name) {
        Node n = children != null ? children.remove(name) : null;
        if (n != null)
            notifyChildrenHandlers(n, true);
        return n;
    }

    public Value removeAttribute(@NonNull String name) {
        return attributes != null ? attributes.remove(name) : null;
    }

    public Value removeConfiguration(@NonNull String name) {
        return configurations != null ? configurations.remove(name) : null;
    }

    public Value getAttribute(@NonNull String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    public Value getConfiguration(@NonNull String name) {
        return configurations != null ? configurations.get(name) : null;
    }

    public Node getChild(@NonNull String name) {
        return children != null ? children.get(name) : null;
    }

    private void update() {
        if (manager != null && isSubscribed()) {
            manager.update(this);
        }
    }

    public synchronized Handler<NodeBooleanTuple> addChildrenHandler(Handler<NodeBooleanTuple> handler) {
        childrenUpdates.add(handler);
        return handler;
    }

    public synchronized void removeChildrenHandler(Handler<NodeBooleanTuple> handler) {
        childrenUpdates.remove(handler);
    }

    private synchronized void notifyChildrenHandlers(Node n, boolean removed) {
        for (Handler<NodeBooleanTuple> handler : childrenUpdates) {
            handler.handle(new NodeBooleanTuple(n, removed));
        }
    }
}
