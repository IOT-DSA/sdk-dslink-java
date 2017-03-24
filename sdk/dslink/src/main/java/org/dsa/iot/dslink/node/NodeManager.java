package org.dsa.iot.dslink.node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.link.Linkable;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.handler.Handler;

/**
 * Handles nodes based on paths.
 *
 * @author Samuel Grenier
 */
public class NodeManager {

    // Fake root to provide a listing on "/"
    private final Node superRoot;
    private final String defaultProfile;

    public NodeManager(Linkable link, String defaultProfile) {
        this.superRoot = new SuperRoot(link, defaultProfile);
        this.defaultProfile = defaultProfile;
    }

    public NodeBuilder createRootNode(String name) {
        return createRootNode(name, defaultProfile);
    }

    public NodeBuilder createRootNode(String name, String profile) {
        NodeBuilder builder = superRoot.createChild(name, false);
        builder.setProfile(profile);
        return builder;
    }

    public Node getSuperRoot() {
        return superRoot;
    }

    public Map<String, Node> getChildren(String path) {
        Node child = getNode(path).getNode();
        if (child == null)
            throw new NoSuchPathException(path);
        return child.getChildren();
    }

    public NodePair getNode(String path) {
        return getNode(path, false);
    }

    public NodePair getNode(String path, boolean create) {
        return getNode(path, create, true);
    }

    public NodePair getNode(String path, boolean create, boolean willThrow) {
        if (path == null)
            throw new NullPointerException("path");
        else if ("/".equals(path))
            return new NodePair(superRoot, null);
        String[] parts = splitPath(path);
        if (parts.length == 1 && StringUtils.isReference(parts[0])) {
            return new NodePair(superRoot, parts[0]);
        }
        Node current = superRoot.getChild(parts[0], false);
        if (create && current == null) {
            NodeBuilder b = superRoot.createChild(parts[0], false);
            b.setProfile(defaultProfile);
            current = b.build();
        }
        for (int i = 1; i < parts.length; i++) {
            if (current == null) {
                break;
            } else if (i + 1 == parts.length && StringUtils.isReference(parts[i])) {
                return new NodePair(current, parts[i]);
            } else {
                Node temp = current.getChild(parts[i], false);
                if (create && temp == null) {
                    NodeBuilder b = current.createChild(parts[i], false);
                    b.setProfile(defaultProfile);
                    temp = b.build();
                }
                current = temp;
            }
        }
        if (current == null && willThrow) {
            throw new NoSuchPathException(path);
        }
        return new NodePair(current, null);
    }

    public static String[] splitPath(String path) {
        return normalizePath(path).split("/");
    }

    public static String normalizePath(String path) {
        return normalizePath(path, false);
    }

    public static String normalizePath(String path, boolean leading) {
        if (path == null || path.isEmpty())
            throw new IllegalArgumentException("path null or empty");
        else if (path.contains("//"))
            throw new IllegalArgumentException("path contains //");
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

    public static class SuperRoot extends Node {
        private static final String ICON = "Icon";

        private SuperRoot(Linkable link, String profile) {
            super("", null, link);
            super.setProfile(profile);
            //sys is needed for the getIcon action
            Node sysNode = createChild("sys")
                    .setSerializable(false)
                    .setHidden(true)
                    .build();
            Action action = new Action(Permission.READ, new Handler<ActionResult>() {
                // Sends the file bytes in response to the invocation.
                @Override
                public void handle(ActionResult event) {
                    Value icon = event.getParameter(ICON);
                    if (icon == null) {
                        throw new NullPointerException("Missing Icon");
                    }
                    String iconString = icon.getString();
                    if ((iconString == null) || iconString.isEmpty()) {
                        throw new NullPointerException("Missing Icon");
                    }
                    DSLinkHandler handler = getLink().getHandler();
                    Table table = event.getTable();
                    table.addRow(Row.make(new Value(handler.getIcon(iconString))));
                    event.setStreamState(StreamState.CLOSED);
                    table.sendReady();
                }
            });
            action.addParameter(new Parameter(ICON, ValueType.STRING));
            action.addResult(new Parameter("Data", ValueType.BINARY));
            sysNode.createChild("getIcon")
                   .setAction(action)
                   .setSerializable(false)
                   .build();
        }
    }

}
