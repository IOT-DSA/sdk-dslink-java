package org.dsa.iot.responder.node;

import lombok.AllArgsConstructor;
import org.dsa.iot.responder.node.exceptions.DuplicateException;
import org.dsa.iot.responder.node.exceptions.NoSuchPathException;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles nodes based on paths.
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class NodeManager {

    private final SubscriptionManager subManager;

    private final Map<String, Node> rootNodes = new HashMap<>();

    public Node createRootNode(String name) {
        return addRootNode(new Node(subManager, null, name));
    }

    public Node addRootNode(Node node) {
        if (rootNodes.containsKey(node.getName())) {
            throw new DuplicateException(node.getName());
        }
        rootNodes.put(node.getName(), node);
        return node;
    }

    public Map<String, Node> getChildren(String path) {
        Node child = getNode(path);
        if (child == null)
            throw new NoSuchPathException(path);
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
