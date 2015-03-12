package org.dsa.iot.dslink.node;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.Pair;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.node.exceptions.DuplicateException;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.dsa.iot.dslink.responder.action.ActionRegistry;

import java.util.Map;

/**
 * Handles nodes based on paths.
 * @author Samuel Grenier
 */
public class NodeManager {

    private final MBassador<Event> bus;
    @NonNull private final ActionRegistry registry;

    // Fake root to provide a listing on "/"
    private final Node superRoot;

    public NodeManager(MBassador<Event> bus, ActionRegistry registry) {
        this.bus = bus;
        this.registry = registry;
        this.superRoot = new Node(bus, null, "", registry) {
            @Override
            protected boolean isRootNode() {
                return true;
            }
        };
    }

    public Node createRootNode(String name) throws DuplicateException {
        return addRootNode(new Node(bus, null, name, registry));
    }

    public Node addRootNode(Node node) throws DuplicateException {
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

    @SneakyThrows
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
