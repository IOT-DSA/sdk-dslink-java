package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

/**
 * @author Samuel Grenier
 */
@SuppressWarnings("unused")
public class NodeBuilder {

    private final Node parent;
    private final Node child;

    public NodeBuilder(Node parent, Node child) {
        this.parent = parent;
        this.child = child;
    }

    public NodeBuilder setDisplayName(String name) {
        child.setDisplayName(name);
        return this;
    }

    public NodeBuilder setProfile(String profile) {
        child.setProfile(profile);
        return this;
    }

    public NodeBuilder setValue(Value value) {
        child.setValue(value);
        return this;
    }

    public NodeBuilder setValueType(ValueType type) {
        child.setValueType(type);
        return this;
    }

    public NodeBuilder setAction(Action action) {
        child.setAction(action);
        return this;
    }

    public NodeBuilder setConfig(String name, Value value) {
        child.setConfig(name, value);
        return this;
    }

    public NodeBuilder setRoConfig(String name, Value value) {
        child.setRoConfig(name, value);
        return this;
    }

    public NodeBuilder setAttribute(String name, Value value) {
        child.setAttribute(name, value);
        return this;
    }

    public NodeBuilder setInterfaces(String interfaces) {
        child.setInterfaces(interfaces);
        return this;
    }

    public NodeBuilder addInterface(String name) {
        child.addInterface(name);
        return this;
    }

    public NodeBuilder setPassword(char[] password) {
        child.setPassword(password);
        return this;
    }

    public NodeBuilder setWritable(Writable writable) {
        child.setWritable(writable);
        return this;
    }

    public NodeBuilder setMetaData(Object object) {
        child.setMetaData(object);
        return this;
    }

    public NodeBuilder setHasChildren(Boolean hasChildren) {
        child.setHasChildren(hasChildren);
        return this;
    }

    public NodeBuilder setHidden(boolean hidden) {
        child.setHidden(hidden);
        return this;
    }

    public NodeBuilder setSerializable(boolean serializable) {
        child.setSerializable(serializable);
        return this;
    }

    public NodeListener getListener() {
        return child.getListener();
    }

    public Node getParent() {
        return parent;
    }

    public Node getChild() {
        return child;
    }

    /**
     * The child will then be added to the parent with the set data. Any
     * subscriptions will then be notified. Note that the parent must not
     * have the child already added or it will just act as a getter.
     * @return Child node
     */
    public Node build() {
        Node node = parent.addChild(child);
        // addChild can be used as a getter, which is useful in scenarios
        // where serialization takes place. However, setting the action
        // before building the node may remove the action override, so in
        // order to ensure that the action is preserved after serialization,
        // the action must be reset on the child node.

        // addChild can return a deserialized node. This results in the action
        // being removed
        node.setMetaData(child.getMetaData());
        node.setAction(child.getAction());
        node.setListener(child.getListener());
        return node;
    }
}
