package org.dsa.iot.dslink.node;

import lombok.Getter;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.Pair;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;

import java.util.Map;

/**
 * Handles nodes based on paths.
 * @author Samuel Grenier
 */
public class NodeManager {

    @Getter
    private final SubscriptionManager subManager;

    private final MBassador<Event> bus;

    // Fake root to provide a listing on "/"
    private final Node superRoot;

    public NodeManager(MBassador<Event> bus, SubscriptionManager subManager) {
        this.bus = bus;
        this.subManager = subManager;
        this.superRoot = new Node(bus, subManager, null, "") {
            @Override
            protected boolean isRootNode() {
                return true;
            }
        };
    }

    public Node createRootNode(String name) {
        return addRootNode(new Node(bus, subManager, null, name));
    }

    public Node addRootNode(Node node) {
        superRoot.addChild(node);
        return node;
    }

    public Map<String, Node> getChildren(String path) {
        Pair<Node, String> child = getNode(path);
        if (child == null)
            throw new NoSuchPathException(path);
        return child.getKey().getChildren();
    }

    public Pair<Node, String> getNode(String path) {
        return getNode(path, false);
    }

    public Pair<Node, String> getNode(String path, boolean create) {
        if ("/".equals(path))
            return new Pair<>(superRoot, null, false);
        String[] parts = splitPath(path);
        Node current = superRoot.getChild(parts[0]);
        if (create && current == null) {
            StringUtils.checkNodeName(parts[0]);
            current = superRoot.createChild(parts[0]);
        }
        for (int i = 1; i < parts.length; i++) {
            if (current == null) {
                break;
            } else if (i + 1 == parts.length && StringUtils.isAttribOrConf(parts[i])) {
                return new Pair<>(current, parts[i], false);
            } else {
                Node temp = current.getChild(parts[i]);
                if (create && temp == null) {
                    StringUtils.checkNodeName(parts[i]);
                    temp = current.createChild(parts[i]);
                }
                current = temp;
            }
        }
        if (current == null)
            throw new NoSuchPathException(path);
        return new Pair<>(current, null, false);
    }

    public static String[] splitPath(String path) {
        return normalizePath(path).split("/");
    }

    public static String normalizePath(String path) {
        return normalizePath(path, false);
    }

    public static String normalizePath(String path, boolean leading) {
        if (path == null || path.isEmpty())
            throw new IllegalArgumentException("path");
        else if ("/".equals(path))
            return path;

        // Examine leading character
        if (!leading && path.startsWith("/"))
            path = path.substring(1);
        else if (leading && !path.startsWith("/"))
            path = "/" + path;

        // Remove ending character
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        return path;
    }
}
