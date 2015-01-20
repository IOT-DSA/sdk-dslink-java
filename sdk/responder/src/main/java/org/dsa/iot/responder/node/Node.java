package org.dsa.iot.responder.node;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.dsa.iot.responder.node.exceptions.DuplicateException;
import org.dsa.iot.responder.node.value.Value;

import java.util.HashMap;
import java.util.Map;

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
    private Value currentValue;

    @Setter
    private boolean invokable; // TODO

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
        if (name.isEmpty() || name.contains("/")
                || name.startsWith("@") || name.startsWith("$"))
            throw new IllegalArgumentException("name");
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
        if (name.isEmpty() || name.contains("/") || name.startsWith("@")
                                                    || name.startsWith("$"))
            throw new IllegalArgumentException("name");
        this.displayName = name;
    }

    public void setCurrentValue(Value value) {
        this.currentValue = value;
        update();
    }

    public void addAttribute(String name, Value value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name");
        if (value == null)
            throw new IllegalArgumentException("value");

        if (attributes == null)
            attributes = new HashMap<>();
        else if (attributes.containsKey(name))
            throw new DuplicateException(name);
        attributes.put(name, value);
    }

    public void addConfiguration(String name, Value value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name");
        if (value == null)
            throw new IllegalArgumentException("value");

        if (configurations == null)
            configurations = new HashMap<>();
        else if (configurations.containsKey(name))
            throw new DuplicateException(name);
        configurations.put(name, value);
    }

    public Node createChild(String name) {
        return addChild(new Node(manager, this, name));
    }

    public Node addChild(Node node) {
        if (children == null)
            children = new HashMap<>();
        else if (children.containsKey(node.name))
            throw new DuplicateException(node.name + "(parent: " + name + ")");
        children.put(node.name, node);
        return node;
    }

    public Node removeChild(Node node) {
        return removeChild(node.name);
    }

    public Node removeChild(String name) {
        return children != null ? children.remove(name) : null;
    }

    public Value removeAttribute(String name) {
        return attributes != null ? attributes.remove(name) : null;
    }

    public Value removeConfiguration(String name) {
        return configurations != null ? configurations.remove(name) : null;
    }

    public Value getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    public Value getConfiguration(String name) {
        return configurations != null ? configurations.get(name) : null;
    }

    public Node getChild(String name) {
        return children != null ? children.get(name) : null;
    }

    private void update() {
        if (manager != null && isSubscribed()) {
            manager.update(this);
        }
    }
}
