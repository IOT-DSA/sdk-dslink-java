package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.StringUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about a node and its data.
 *
 * @author Samuel Grenier
 */
public class Node {

    private static final String[] BANNED_CHARS = new String[]{
            ".", "/", "\\", "?", "%", "*", ":", "|", "<", ">", "$", "@"
    };

    private final WeakReference<Node> parent;
    private final String path;
    private final String name;

    private String displayName;
    private Map<String, Node> children;
    private Map<String, Value> configs;
    private Map<String, Value> attribs;
    private Action action;

    /**
     * Constructs a node object.
     *
     * @param name   Name of the node
     * @param parent Parent of this node
     */
    public Node(String name, Node parent) {
        this.parent = new WeakReference<>(parent);
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
     * @return Name of the node
     */
    public String getName() {
        return name;
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
     * @return Formalized path of this node.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return Children of the node, can be null
     */
    public synchronized Map<String, Node> getChildren() {
        return children != null ? new HashMap<>(children) : null;
    }

    /**
     * @param name Child name
     * @return Child, or null if non-existent
     */
    public synchronized Node getChild(String name) {
        return children != null ? children.get(name) : null;
    }

    /**
     * The child will be created if the node doesn't exist. If the child
     * already exists then it will be returned and no new node will be
     * created. This can be used as a special getter.
     *
     * @param name Name of the node
     * @return The node
     */
    public synchronized Node createChild(String name) {
        if (children == null) {
            children = new HashMap<>();
        } else if (children.containsKey(name)) {
            return children.get(name);
        }

        Node node = new Node(name, this);
        children.put(name, node);
        return node;
    }

    /**
     * @param name Node to remove
     * @return The node if it existed
     */
    public synchronized Node removeChild(String name) {
        return children != null ? children.remove(name) : null;
    }

    /**
     * @return The configurations in this node.
     */
    public synchronized Map<String, Value> getConfigurations() {
        return configs != null ? new HashMap<>(configs) : null;
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
        if ("params".equals(name) || "columns".equals(name)) {
            String err = "Use the action API to set parameters and columns";
            throw new IllegalArgumentException(err);
        }
        return configs.put(name, value);
    }

    /**
     * @return The attributes in this node.
     */
    public synchronized Map<String, Value> getAttributes() {
        return attribs != null ? new HashMap<>(attribs) : null;
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
        return attribs.put(name, value);
    }

    /**
     * @return Action this node can invoke
     */
    public synchronized Action getAction() {
        return action;
    }

    /**
     * Sets the action of the node
     *
     * @param action Action to set
     */
    public synchronized void setAction(Action action) {
        this.action = action;
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
}
