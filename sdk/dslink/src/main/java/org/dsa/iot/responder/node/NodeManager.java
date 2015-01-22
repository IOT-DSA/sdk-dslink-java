package org.dsa.iot.responder.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.core.StringUtils;
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
        NodeStringTuple child = getNode(path);
        if (child == null)
            throw new NoSuchPathException(path);
        return child.getNode().getChildren();
    }

    public NodeStringTuple getNode(String path) {
        if (path == null || path.isEmpty())
            throw new IllegalArgumentException("path");
        else if (path.startsWith("/"))
            path = path.substring(1);
        String[] parts = path.split("/");
        Node current = rootNodes.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (current == null)
                return null;
            else if (i + 1 == parts.length && StringUtils.isAttribOrConf(parts[i]))
                return new NodeStringTuple(current, parts[i]);
            else
                current = current.getChild(parts[i]);
        }
        return current != null ? new NodeStringTuple(current, null) : null;
    }

    @Getter
    @AllArgsConstructor
    public static class NodeStringTuple {

        /**
         * Node is always populated
         */
        @NonNull
        private final Node node;

        /**
         * Only populated if the path is a reference to an attribute or
         * configuration.
         */
        private final String string;

    }

    @Getter
    @AllArgsConstructor
    public static class NodeBooleanTuple {

        @NonNull
        private final Node node;

        private final boolean bool;
    }
}
