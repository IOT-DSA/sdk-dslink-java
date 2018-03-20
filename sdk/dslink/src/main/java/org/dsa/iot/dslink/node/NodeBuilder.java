package org.dsa.iot.dslink.node;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

        node.setSerializable(child.isSerializable());
        node.setProfile(child.getProfile());
        node.setMetaData(child.getMetaData());
        {
            // addChild can return a pre-existing node. This results in the
            // action being removed. The action is preserved to ensure that
            // deserialized nodes can keep their actions without constantly
            // being set. The actions are compared to their previous action
            // to prevent unnecessary updates to the network.
            Action parentAct = node.getAction();
            Action childAct = child.getAction();
            if (parentAct != childAct) {
                node.setAction(child.getAction());
            }
        }
        node.setListener(child.getListener());
        node.setDisplayName(child.getDisplayName());
        node.setValueType(child.getValueType());
        node.setValue(child.getValue());
        node.setPassword(child.getPassword());
        node.setWritable(child.getWritable());
        node.setHasChildren(child.getHasChildren());
        node.setHidden(child.isHidden());
        node.setSerializable(child.isSerializable());
        Map<String, Value> configs = child.getConfigurations();
        Map<String, Value> roconfigs = child.getRoConfigurations();
        Map<String, Value> attrs = child.getAttributes();
        Set<String> interfaces = child.getInterfaces();
        if (configs != null) {
	        for (Entry<String, Value> entry: configs.entrySet()) {
	        	node.setConfig(entry.getKey(), entry.getValue());
	        }
        }
        if (roconfigs != null) {
	        for (Entry<String, Value> entry: roconfigs.entrySet()) {
	        	node.setRoConfig(entry.getKey(), entry.getValue());
	        }
        }
        if (attrs != null) {
	        for (Entry<String, Value> entry: attrs.entrySet()) {
	        	node.setAttribute(entry.getKey(), entry.getValue());
	        }
        }
        if (interfaces != null) {
	        for (String _interface: interfaces) {
	        	node.addInterface(_interface);
	        }
        }
        return node;
    }
}
