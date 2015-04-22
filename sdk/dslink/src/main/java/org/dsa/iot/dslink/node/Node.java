package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.link.Linkable;
import org.dsa.iot.dslink.node.NodeListener.ValueUpdate;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.value.Value;
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

    private final WeakReference<Node> parent;
    private final NodeListener listener;
    private final Linkable link;
    private final String path;
    private final String name;

    private Map<String, Node> children;

    private Map<String, Value> roConfigs;
    private Map<String, Value> configs;
    private Map<String, Value> attribs;

    private String displayName;
    private String profile;
    private Set<String> mixins;
    private Set<String> interfaces;
    private Action action;
    private Value value;
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
        this.listener = new NodeListener();
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
        setProfile(profile);
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
     * The listener API provides functionality for listening to changes
     * that occur within a node.
     *
     * @return The node's listener.
     */
    public NodeListener getListener() {
        return listener;
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
        // TODO: confirm profile exists in the node manager
    }

    /**
     * @return The profile this node belongs to
     */
    public String getProfile() {
        return profile;
    }

    public synchronized void addMixin(String mixin) {
        if (mixin == null) {
            throw new NullPointerException("mixin");
        } else if (mixins == null) {
            mixins = new HashSet<>();
        }
        mixins.add(mixin);
    }

    public synchronized void removeMixin(String mixin) {
        if (mixin == null) {
            throw new NullPointerException("mixin");
        } else if (mixins != null) {
            mixins.remove(mixin);
        }
    }

    public synchronized void setMixins(String mixin) {
        if (mixin == null) {
            throw new NullPointerException("mixin");
        } else if (mixins == null) {
            mixins = new HashSet<>();
        }
        String[] split = mixin.split("\\|");
        Collections.addAll(mixins, split);
    }

    public synchronized Set<String> getMixins() {
        return mixins != null ? new HashSet<>(mixins) : null;
    }

    public synchronized void addInterface(String _interface) {
        if (_interface == null) {
            throw new NullPointerException("_interface");
        } else if (interfaces == null) {
            interfaces = new HashSet<>();
        }
        interfaces.add(_interface);
    }

    public synchronized void removeInterface(String mixin) {
        if (mixin == null) {
            throw new NullPointerException("mixin");
        } else if (interfaces != null) {
            interfaces.remove(mixin);
        }
    }

    public synchronized void setInterfaces(String _interface) {
        if (_interface == null) {
            throw new NullPointerException("_interface");
        } else if (interfaces == null) {
            interfaces = new HashSet<>();
        }
        String[] split = _interface.split("\\|");
        Collections.addAll(interfaces, split);
    }

    public synchronized Set<String> getInterfaces() {
        return interfaces != null ? new HashSet<>(interfaces) : null;
    }

    public synchronized void setValue(Value value) {
        if (value != null) {
            value.setImmutable();
        }
        this.value = value;
        listener.postValueUpdate(value);

        if (link != null) {
            SubscriptionManager manager = link.getSubscriptionManager();
            if (manager != null) {
                manager.postValueUpdate(this);
            }
        }
    }

    /**
     * @return The value of the node.
     */
    public synchronized Value getValue() {
        return value;
    }

    /**
     * @return Children of the node, can be null
     */
    public synchronized Map<String, Node> getChildren() {
        return children != null ? new HashMap<>(children) : null;
    }

    /**
     * Clears the children in the node.
     */
    public synchronized void clearChildren() {
        if (children != null) {
            for (Node child : getChildren().values()) {
                removeChild(child);
            }
        }
    }

    /**
     * @param name Child name
     * @return Child, or null if non-existent
     */
    public synchronized Node getChild(String name) {
        return children != null ? children.get(name) : null;
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
    public synchronized Node addChild(Node node) {
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

        node.setProfile(profile);
        children.put(name, node);
        if (manager != null) {
            manager.postChildUpdate(node, false);
        }
        return node;
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
    public synchronized Node removeChild(String name) {
        Node child = children != null ? children.remove(name) : null;
        SubscriptionManager manager = null;
        if (link != null) {
            manager = link.getSubscriptionManager();
        }
        if (child != null && manager != null) {
            manager.postChildUpdate(child, true);
            manager.removeValueSub(child);
        }
        return child;
    }

    /**
     * @return The configurations in this node.
     */
    public synchronized Map<String, Value> getConfigurations() {
        return configs != null ? new HashMap<>(configs) : null;
    }

    /**
     * @param name Configuration name to remove
     * @return Configuration value, or null if it didn't exist
     */
    public synchronized Value removeConfig(String name) {
        Value ret = configs != null ? configs.remove(name) : null;
        if (ret != null) {
            ValueUpdate update = new ValueUpdate(name, ret, true);
            listener.postConfigUpdate(update);
        }
        return ret;
    }

    /**
     * @param name Configuration name to get
     * @return Value of the configuration, if it exists
     */
    public synchronized Value getConfig(String name) {
        return configs != null ? configs.get(name) : null;
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
    public synchronized Value setConfig(String name, Value value) {
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
            case "mixin":
            case "invokable":
            case "interface":
            case "permission":
            case "result":
                String err = "Config `" + name + "` has special methods"
                        + " for setting these properties";
                throw new IllegalArgumentException(err);
        }
        value.setImmutable();
        ValueUpdate update = new ValueUpdate(name, value, false);
        listener.postConfigUpdate(update);
        return configs.put(name, value);
    }

    /**
     * @return The read-only configurations in this node.
     */
    public synchronized Map<String, Value> getRoConfigurations() {
        return roConfigs != null ? new HashMap<>(roConfigs) : null;
    }

    /**
     * Removes a read-only configuration.
     *
     * @param name Name of the configuration.
     * @return Previous value of the configuration.
     */
    public synchronized Value removeRoConfig(String name) {
        return roConfigs != null ? roConfigs.remove(name) : null;
    }

    /**
     * Retrieves a read-only configuration.
     *
     * @param name Name of the configuration.
     * @return The value of the configuration name, if any.
     */
    public synchronized Value getRoConfig(String name) {
        return roConfigs != null ? roConfigs.get(name) : null;
    }

    /**
     * Sets a read-only configuration.
     *
     * @param name Name of the configuration.
     * @param value Value to set.
     * @return The previous value, if any.
     */
    public synchronized Value setRoConfig(String name, Value value) {
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

    /**
     * @return The attributes in this node.
     */
    public synchronized Map<String, Value> getAttributes() {
        return attribs != null ? new HashMap<>(attribs) : null;
    }

    /**
     * @param name Attribute name to remove.
     * @return Attribute value or null if it didn't exist
     */
    public synchronized Value removeAttribute(String name) {
        Value ret = attribs != null ? attribs.get(name) : null;
        if (ret != null) {
            ValueUpdate update = new ValueUpdate(name, ret, true);
            listener.postAttributeUpdate(update);
        }
        return ret;
    }

    /**
     * @param name Attribute name to get
     * @return Value of the attribute, if it exists
     */
    public synchronized Value getAttribute(String name) {
        return attribs != null ? attribs.get(name) : null;
    }

    /**
     * @param name  Name of the attribute
     * @param value Value to set
     * @return The previous attribute value, if any
     */
    public synchronized Value setAttribute(String name, Value value) {
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

    /**
     * @return Action this node can invoke
     */
    public synchronized Action getAction() {
        return action;
    }

    /**
     * Sets the action of the node.
     *
     * @param action Action to set. Use {@code null} to remove an action.
     */
    public synchronized void setAction(Action action) {
        this.action = action;
    }

    /**
     * Gets the password the node is configured to use. This is necessary
     * for authentication to servers.
     *
     * @return Password the node is configured to use.
     */
    public synchronized char[] getPassword() {
        return pass != null ? pass.clone() : null;
    }

    /**
     * If this node accesses servers and requires authentication, the password
     * must be set here. This will censor the password from being retrieved
     * through the responder.
     *
     * @param password Password to set.
     */
    public synchronized void setPassword(char[] password) {
        this.pass = password != null ? password.clone() : null;
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
