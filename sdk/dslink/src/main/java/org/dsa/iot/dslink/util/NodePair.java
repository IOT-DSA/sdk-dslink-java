package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.node.Node;

/**
 * Holds a pair between a node reference and a configuration or attribute in
 * the path.
 * @author Samuel Grenier
 */
public class NodePair {

    /**
     * Path of the node
     */
    private final Node node;

    /**
     * Reference to a configuration or attribute.
     */
    private final String reference;

    public NodePair(Node node, String reference) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        this.node = node;
        this.reference = reference;
    }

    public Node getNode() {
        return node;
    }

    public String getReference() {
        return reference;
    }
}
