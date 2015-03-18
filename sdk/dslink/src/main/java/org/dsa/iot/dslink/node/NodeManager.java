package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;

import java.util.Map;

/**
 * Handles nodes based on paths.
 *
 * @author Samuel Grenier
 */
public class NodeManager {

    // Fake root to provide a listing on "/"
    private final Node superRoot;

    public NodeManager() {
        this.superRoot = new Node(null, null);
    }

    public Node createRootNode(String name) {
        return superRoot.createChild(name);
    }

    public Map<String, Node> getChildren(String path) {
        Node child = getNode(path);
        if (child == null)
            throw new NoSuchPathException(path);
        return child.getChildren();
    }

    public Node getNode(String path) {
        return getNode(path, false);
    }

    public Node getNode(String path, boolean create) {
        if (path == null)
            throw new NullPointerException("path");
        else if ("/".equals(path))
            return superRoot;
        String[] parts = splitPath(path);
        Node current = superRoot.getChild(parts[0]);
        if (create && current == null) {
            current = superRoot.createChild(Node.checkName(parts[0]));
        }
        for (int i = 1; i < parts.length; i++) {
            if (current == null) {
                break;
            } else {
                Node temp = current.getChild(parts[i]);
                if (create && temp == null) {
                    temp = current.createChild(Node.checkName(parts[i]));
                }
                current = temp;
            }
        }
        if (current == null)
            throw new NoSuchPathException(path);
        return current;
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
