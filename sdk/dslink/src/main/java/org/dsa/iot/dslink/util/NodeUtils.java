package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class NodeUtils {

    /**
     * When using a {@link NodeBuilder} there is a possibility that the node
     * already exists. This can be as a result of serialization. This will
     * retrieve the underlying value from the real node if that node exists.
     * If the node doesn't currently exist then configuration set on the
     * {@link NodeBuilder} will be returned.
     *
     * @param b Node that has not yet been built.
     * @param name Name of the read-only configuration.
     * @return Value of the configuration.
     */
    public static Value getRoConfig(NodeBuilder b, String name) {
        Node n = b.getParent().getChild(b.getChild().getName(), false);
        if (n != null) {
            return n.getRoConfig(name);
        }
        return b.getChild().getRoConfig(name);
    }
}
