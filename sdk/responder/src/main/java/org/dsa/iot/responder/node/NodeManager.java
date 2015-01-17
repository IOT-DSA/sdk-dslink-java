package org.dsa.iot.responder.node;

import org.dsa.iot.responder.node.exceptions.DuplicateException;
import org.dsa.iot.responder.node.exceptions.NoSuchPath;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles nodes based on paths.
 * @author Samuel Grenier
 */
public class NodeManager {

    private final Map<String, Node> rootNodes = new HashMap<>();

    public Node addRootNode(Node node) {
        if (rootNodes.containsKey(node.name)) {
            throw new DuplicateException(node.name);
        }
        rootNodes.put(node.name, node);
        return node;
    }

    public Map<String, Node> getChildren(String path) {
        Node child = getNode(path);
        if (child == null)
            throw new NoSuchPath(path);
        return child.getChildren();
    }

    public Node getNode(String path) {
        if (path == null || path.isEmpty())
            throw new IllegalArgumentException("path");
        else if (path.startsWith("/"))
            path = path.substring(1);
        String[] parts = path.split("/");
        Node current = rootNodes.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (current == null)
                return null;
            current = current.getChild(parts[i]);
        }
        return current;
    }
}
