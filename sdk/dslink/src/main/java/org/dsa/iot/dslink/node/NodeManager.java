package org.dsa.iot.dslink.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.StringUtils;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;

import java.util.Map;

/**
 * Handles nodes based on paths.
 * @author Samuel Grenier
 */
public class NodeManager {

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
        node.init();
        return node;
    }

    public Map<String, Node> getChildren(String path) {
        NodeStringTuple child = getNode(path);
        if (child == null)
            throw new NoSuchPathException(path);
        return child.getNode().getChildren();
    }

    public NodeStringTuple getNode(String path) {
        return getNode(path, false);
    }

    public NodeStringTuple getNode(String path, boolean create) {
        if (path == null || path.isEmpty())
            throw new IllegalArgumentException("path");
        else if ("/".equals(path))
            return new NodeStringTuple(superRoot, null);
        else if (path.startsWith("/"))
            path = path.substring(1);
        String[] parts = path.split("/");
        Node current = superRoot.getChild(parts[0]);
        if (create && current == null) {
            StringUtils.checkNodeName(parts[0]);
            current = superRoot.createChild(parts[0]);
        }
        for (int i = 1; i < parts.length; i++) {
            if (current == null) {
                break;
            } else if (i + 1 == parts.length && StringUtils.isAttribOrConf(parts[i])) {
                return new NodeStringTuple(current, parts[i]);
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
        return new NodeStringTuple(current, null);
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
}
