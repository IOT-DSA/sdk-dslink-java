package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.link.Linkable;
import org.dsa.iot.dslink.node.NodeListener.ValueUpdate;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.StringUtils;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Contains information about a node and its data.
 *
 * @author Samuel Grenier
 */
public class Node {

    static final String[] BANNED_CHARS = new String[] {
            ".", "/", "\\", "?", "%", "*", ":", "|", "<", ">", "$", "@"
    };

    private final Object roConfigLock = new Object();
    private final Object configLock = new Object();

    private final Object attributeLock = new Object();
    private final Object interfaceLock = new Object();
    private final Object childrenLock = new Object();
    private final Object passwordLock = new Object();
    private final Object valueLock = new Object();

    private final WeakReference<Node> parent;
    private final Linkable link;
    private final String path;
    private final String name;

    private boolean serializable = true;
    private NodeListener listener;
    private Map<String, Node> children;
    private Writable writable;

    private Map<String, Value> roConfigs;
    private Map<String, Value> configs;
    private Map<String, Value> attribs;

    private ValueType valueType;
    private Value value;

    private String displayName;
    private String profile;
    private Set<String> interfaces;
    private Action action;
    private char[] pass;

    /**
     * Constructs a node object.
     *
     * @param name   Name of the node
     * @param parent Parent of this node
     * @param link Linkable class the node is handled on
     */
    public Node(String name, Node parent, Linkable link) {
        this.parent = new WeakReference<>(parent);
        this.listener = new NodeListener(this);
        this.link = link;
        if (parent != null) {
            this.name = checkName(name);
            this.path = parent.getPath() + "/" + name;
        } else {
            if (name != null) {
                checkName(name);
                this.path = "/" + name;
                this.name = name;
            } else {
                this.path = "";
                this.name = "";
            }
        }
    }

    /**
     * @return Parent of this node, can be null if the parent was garbage
     *         collected or there is no parent.
     */
    public Node getParent() {
        return parent.get();
    }

    /**
     * @return The link this node is attached to.
     */
    public Linkable getLink() {
        return link;
    }

    /**
     * @return Name of the node
     */
    public String getName() {
        return name;
    }

    /**
     * @return Formalized path of this node.
     */
    public String getPath() {
        return path;
    }

    /**
     * @param name Display name of the node to set
     */
    public void setDisplayName(String name) {
        displayName = checkName(name);
    }

    /**
     * @return Display name of the node
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the profile of the node
     *
     * @param profile Profile to set
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * @return The profile this node belongs to
     */
    public String getProfile() {
        return profile;
    }

    /**
     * The listener API provides functionality for listening to changes
     * that occur within a node.
     *
     * @return The node's listener.
     */
    public NodeListener getListener() {
        return listener;
    }

    /**
     * Used to set the listener to allow the node builder to override
     * the internal listener.
     *
     * @param listener Listener to set.
     */
    protected void setListener(NodeListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        this.listener = listener;
    }

    public void addInterface(String _interface) {
        synchronized (interfaceLock) {
            if (_interface == null) {
                throw new NullPointerException("_interface");
            } else if (interfaces == null) {
                interfaces = new HashSet<>();
            }
            interfaces.add(_interface);
        }
    }

    @SuppressWarnings("unused")
    public void removeInterface(String _interface) {
        synchronized (interfaceLock) {
            if (_interface == null) {
                throw new NullPointerException("_interface");
            } else if (interfaces != null) {
                interfaces.remove(_interface);
            }
        }
    }

    public void setInterfaces(String _interface) {
        synchronized (interfaceLock) {
            if (_interface == null) {
                throw new NullPointerException("_interface");
            } else if (interfaces == null) {
                interfaces = new HashSet<>();
            }
            String[] split = _interface.split("\\|");
            Collections.addAll(interfaces, split);
        }
    }

    public Set<String> getInterfaces() {
        synchronized (interfaceLock) {
            return interfaces != null ? new HashSet<>(interfaces) : null;
        }
    }

    public void setValue(Value value) {
        synchronized (valueLock) {
            ValueType type = valueType;
            if (type == null) {
                String err = "Value type not set on node (" + getPath() + ")";
                throw new RuntimeException(err);
            }
            if (value != null) {
                if (type.compare(ValueType.ENUM)) {
                    if (!value.getType().compare(ValueType.STRING)) {
                        String err = "[" + getPath() + "] ";
                        err += "Node has enum value type, value must be string";
                        throw new RuntimeException(err);
                    } else if (!type.getEnums().contains(value.getString())) {
                        String err = "[" + getPath() + "] ";
                        err += "New value does not contain a valid enum value";
                        throw new RuntimeException(err);
                    }
                } else if (!type.compare(ValueType.DYNAMIC)
                            && type != value.getType()) {
                    String err = "[" + getPath() + "] ";
                    err += "Expected value type ";
                    err += "'" + type.toJsonString() + "' ";
                    err += "got '" + value.getType().toJsonString() + "'";
                    throw new RuntimeException(err);
                }

                value.setImmutable();
            }

            if ((this.value != null && this.value.equals(value))
                    || listener.postValueUpdate(this.value, value)) {
                return;
            }

            this.value = value;
            if (link != null) {
                SubscriptionManager manager = link.getSubscriptionManager();
                if (manager != null) {
                    manager.postValueUpdate(this);
                }
            }
        }
    }

    /**
     * @return The value of the node.
     */
    public Value getValue() {
        synchronized (valueLock) {
            return value;
        }
    }

    public void setValueType(ValueType type) {
        this.valueType = type;
    }

    public ValueType getValueType() {
        return valueType;
    }

    /**
     * @param writable Permission level required to write.
     */
    public void setWritable(Writable writable) {
        this.writable = writable;
    }

    /**
     * @return The permission level needed to be writable.
     */
    public Writable getWritable() {
        return writable;
    }

    /**
     * @return Children of the node, can be null
     */
    public Map<String, Node> getChildren() {
        synchronized (childrenLock) {
            return children != null ? new HashMap<>(children) : null;
        }
    }

    /**
     * Clears the children in the node.
     */
    @SuppressWarnings("unused")
    public void clearChildren() {
        synchronized (childrenLock) {
            if (children != null) {
                for (Node child : getChildren().values()) {
                    removeChild(child);
                }
            }
        }
    }

    /**
     * @param name Child name
     * @return Child, or null if non-existent
     */
    public Node getChild(String name) {
        synchronized (childrenLock) {
            return children != null ? children.get(name) : null;
        }
    }

    /**
     * Creates a child. The profile in the child node will be
     * inherited from the parent.
     *
     * @param name Name of the child
     * @return builder
     */
    public NodeBuilder createChild(String name) {
        return createChild(name, profile);
    }

    /**
     * Creates a node builder to allow setting up the node data before
     * any list subscriptions can be notified.
     *
     * @param name Name of the child.
     * @param profile Profile to set on the child
     * @return builder
     * @see NodeBuilder#build
     */
    public NodeBuilder createChild(String name, String profile) {
        NodeBuilder b = new NodeBuilder(this, new Node(name, this, link));
        if (profile != null) {
            b.setProfile(profile);
        }
        return b;
    }

    /**
     * The child will be added if the node doesn't exist. If the child
     * already exists then it will be returned and no new node will be
     * created. This can be used as a special getter.
     *
     * @param node Child node to add.
     * @return The node
     */
    public Node addChild(Node node) {
        synchronized (childrenLock) {
            String name = node.getName();
            if (children == null) {
                children = new HashMap<>();
            } else if (children.containsKey(name)) {
                return children.get(name);
            }

            SubscriptionManager manager = null;
            if (link != null) {
                manager = link.getSubscriptionManager();
            }

            if (node.getProfile() == null) {
                node.setProfile(profile);
            }
            children.put(name, node);
            if (manager != null) {
                manager.postChildUpdate(node, false);
            }
            return node;
        }
    }

    /**
     * @param node Node to remove.
     * @return The node if it existed.
     */
    public Node removeChild(Node node) {
        if (node != null) {
            return removeChild(node.getName());
        } else {
            return null;
        }
    }

    /**
     * @param name Node to remove.
     * @return The node if it existed.
     */
    public Node removeChild(String name) {
        synchronized (childrenLock) {
            Node child = children != null ? children.remove(name) : null;
            SubscriptionManager manager = null;
            if (link != null) {
                manager = link.getSubscriptionManager();
            }

            if (child != null) {
                child.getListener().kill();
                if (manager != null) {
                    manager.postChildUpdate(child, true);
                    manager.removeValueSub(child);
                    manager.removePathSub(child);
                }
            }
            return child;
        }
    }

    /**
     * @return The configurations in this node.
     */
    public Map<String, Value> getConfigurations() {
        synchronized (configLock) {
            return configs != null ? new HashMap<>(configs) : null;
        }
    }

    /**
     * @param name Configuration name to get
     * @return Value of the configuration, if it exists
     */
    public Value getConfig(String name) {
        synchronized (configLock) {
            return configs != null ? configs.get(name) : null;
        }
    }

    /**
     * @param name Configuration name to remove
     * @return Configuration value, or null if it didn't exist
     */
    public Value removeConfig(String name) {
        synchronized (configLock) {
            Value ret = configs != null ? configs.remove(name) : null;
            if (ret != null) {
                ValueUpdate update = new ValueUpdate(name, ret, true);
                listener.postConfigUpdate(update);
            }
            return ret;
        }
    }

    /**
     * The name will be checked for validity. Certain names that are set
     * through other APIs cannot be set here, otherwise it will throw an
     * exception.
     *
     * @param name  Name of the configuration
     * @param value Value to set
     * @return The previous configuration value, if any
     * @see Action
     */
    public Value setConfig(String name, Value value) {
        synchronized (configLock) {
            name = checkName(name);
            if (value == null) {
                throw new NullPointerException("value");
            } else if (configs == null) {
                configs = new HashMap<>();
            }
            switch (name) {
                case "params":
                case "columns":
                case "name":
                case "is":
                case "invokable":
                case "interface":
                case "permission":
                case "result":
                case "type":
                case "writable":
                    String err = "Config `" + name + "` has special methods"
                            + " for setting these properties";
                    throw new IllegalArgumentException(err);
            }
            value.setImmutable();
            ValueUpdate update = new ValueUpdate(name, value, false);
            NodeListener listener = this.listener;
            if (listener != null) {
                listener.postConfigUpdate(update);
            }
            return configs.put(name, value);
        }
    }

    /**
     * @return The read-only configurations in this node.
     */
    public Map<String, Value> getRoConfigurations() {
        synchronized (roConfigLock) {
            return roConfigs != null ? new HashMap<>(roConfigs) : null;
        }
    }

    /**
     * Removes a read-only configuration.
     *
     * @param name Name of the configuration.
     * @return Previous value of the configuration.
     */
    public Value removeRoConfig(String name) {
        synchronized (roConfigLock) {
            return roConfigs != null ? roConfigs.remove(name) : null;
        }
    }

    /**
     * Retrieves a read-only configuration.
     *
     * @param name Name of the configuration.
     * @return The value of the configuration name, if any.
     */
    @SuppressWarnings("unused")
    public Value getRoConfig(String name) {
        synchronized (roConfigLock) {
            return roConfigs != null ? roConfigs.get(name) : null;
        }
    }

    /**
     * Sets a read-only configuration.
     *
     * @param name Name of the configuration.
     * @param value Value to set.
     * @return The previous value, if any.
     */
    public Value setRoConfig(String name, Value value) {
        synchronized (roConfigLock) {
            name = checkName(name);
            if (value == null) {
                throw new NullPointerException("value");
            } else if (roConfigs == null) {
                roConfigs = new HashMap<>();
            }

            switch (name) {
                case "password":
                    String err = "Config `" + name + "` has special methods"
                            + " for setting these properties";
                    throw new IllegalArgumentException(err);
            }

            return roConfigs.put(name, value);
        }
    }

    /**
     * @return The attributes in this node.
     */
    public Map<String, Value> getAttributes() {
        synchronized (attributeLock) {
            return attribs != null ? new HashMap<>(attribs) : null;
        }
    }

    /**
     * @param name Attribute name to remove.
     * @return Attribute value or null if it didn't exist
     */
    public Value removeAttribute(String name) {
        synchronized (attributeLock) {
            Value ret = attribs != null ? attribs.remove(name) : null;
            if (ret != null) {
                ValueUpdate update = new ValueUpdate(name, ret, true);
                listener.postAttributeUpdate(update);
            }

            return ret;
        }
    }

    /**
     * @param name Attribute name to get
     * @return Value of the attribute, if it exists
     */
    public Value getAttribute(String name) {
        synchronized (attributeLock) {
            return attribs != null ? attribs.get(name) : null;
        }
    }

    /**
     * @param name  Name of the attribute
     * @param value Value to set
     * @return The previous attribute value, if any
     */
    public Value setAttribute(String name, Value value) {
        synchronized (attributeLock) {
            name = checkName(name);
            if (value == null) {
                throw new NullPointerException("value");
            } else if (attribs == null) {
                attribs = new HashMap<>();
            }
            value.setImmutable();
            ValueUpdate update = new ValueUpdate(name, value, false);
            listener.postAttributeUpdate(update);
            return attribs.put(name, value);
        }
    }

    /**
     * @return Action this node can invoke
     */
    public Action getAction() {
        return action;
    }

    /**
     * Sets the action of the node.
     *
     * @param action Action to set. Use {@code null} to remove an action.
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * Gets the password the node is configured to use. This is necessary
     * for authentication to servers.
     *
     * @return Password the node is configured to use.
     */
    public char[] getPassword() {
        synchronized (passwordLock) {
            return pass != null ? pass.clone() : null;
        }
    }

    /**
     * If this node accesses servers and requires authentication, the password
     * must be set here. This will censor the password from being retrieved
     * through the responder.
     *
     * @param password Password to set.
     */
    public void setPassword(char[] password) {
        synchronized (passwordLock) {
            this.pass = password != null ? password.clone() : null;
        }
    }

    /**
     * Creates a fake node builder that wraps its methods around
     * this node. This allows fitting a {@link Node} into a {@link NodeBuilder}
     * when necessary.
     *
     * @return A fake node builder.
     */
    public NodeBuilder createFakeBuilder() {
        return new NodeBuilder(getParent(), this) {
            @Override
            public Node build() {
                return Node.this;
            }
        };
    }

    /**
     * If this node is not serializable, none of the children will be either
     * by default.
     *
     * @return Whether this node should be serialized or not
     */
    public boolean isSerializable() {
        return serializable;
    }

    /**
     * Sets whether this node and its children should be serialized.
     *
     * @param serializable Whether this node can be serialized.
     */
    @SuppressWarnings("unused")
    public void setSerializable(boolean serializable) {
        this.serializable = serializable;
    }

    /**
     * Checks the string and then returns it. An exception is thrown if the
     * name is invalid in any way.
     *
     * @param name Name to check
     * @return Name
     */
    public static String checkName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name");
        } else if (StringUtils.contains(name, BANNED_CHARS)) {
            throw new IllegalArgumentException("invalid name: " + name);
        }
        return name;
    }

    /**
     * @return The banned characters not allowed to be in names.
     */
    public static String[] getBannedCharacters() {
        return BANNED_CHARS.clone();
    }
}
