package org.dsa.iot.responder.node;

import org.dsa.iot.responder.node.exceptions.DuplicateException;
import org.dsa.iot.responder.node.value.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Node {

    private final Node parent;

    private Map<String, Node> children;
    private Map<String, Value> attributes;
    private Map<String, Value> configurations;

    public final String name;
    private String displayName;

    /**
     *
     * @param parent The parent of this node, or null if a root node
     * @param name The name of this node
     */
    public Node(Node parent, String name) {
        if (name == null
                || name.isEmpty()
                || name.contains("/")
                || name.startsWith("@")
                || name.startsWith("$"))
            throw new IllegalArgumentException("name");
        this.parent = parent;
        this.name = name;
    }

    public void setDisplayName(String name) {
        if (name == null) {
            displayName = null;
            return;
        }
        if (name.isEmpty()
                || name.contains("/")
                || name.startsWith("@")
                || name.startsWith("$"))
            throw new IllegalArgumentException("name");
        this.displayName = name;
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
        return addChild(new Node(this, name));
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
        if (children != null)
            return children.remove(name);
        return null;
    }

    public Value removeAttribute(String name) {
        if (attributes != null)
            return attributes.remove(name);
        return null;
    }

    public Value removeConfiguration(String name) {
        if (configurations != null)
            return configurations.remove(name);
        return null;
    }

    /**
     * @return Whether the node can be invoked or not
     */
    public boolean isInvokable() {
        return false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPath() {
        if (parent == null) {
            // This is a root node
            return "/" + name;
        }
        return parent.getPath() + "/" + name;
    }

    public Value getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    public Map<String, Value> getAttributes() {
        return attributes;
    }

    public Value getConfiguration(String name) {
        return configurations != null ? configurations.get(name) : null;
    }

    public Map<String, Value> getConfigurations() {
        return configurations;
    }

    public Node getChild(String name) {
        return children != null ? children.get(name) : null;
    }

    public Map<String, Node> getChildren() {
        return children;
    }
}
