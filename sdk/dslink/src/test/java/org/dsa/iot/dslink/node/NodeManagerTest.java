package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the node manager.
 *
 * @author Samuel Grenier
 */
public class NodeManagerTest {

    /**
     * Tests node additions to the node manager. Also exercises node path
     * building when retrieving nodes.
     */
    @Test
    public void nodeAdditions() {
        NodeManager manager = new NodeManager(null, "node");
        manager.createRootNode("A", "node");

        Assert.assertNotNull(manager.getNode("A"));
        Assert.assertNotNull(manager.getNode("/A"));
        Assert.assertNotNull(manager.getNode("/A/"));

        manager.createRootNode("A", "node").createChild("B", "node");
        Assert.assertNotNull(manager.getNode("/A/B"));
    }

    /**
     * Tests that node removals are completely out of the node manager.
     */
    @Test(expected = NoSuchPathException.class)
    public void nodeRemovals() {
        NodeManager manager = new NodeManager(null, "node");
        manager.createRootNode("A", "node");
        manager.getNode("/").getNode().removeChild("A");
        manager.getNode("/A");
    }

    @Test(expected = NullPointerException.class)
    public void nullPath() {
        NodeManager manager = new NodeManager(null, "node");
        manager.getNode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badPath() {
        NodeManager manager = new NodeManager(null, "node");
        manager.getNode("/A//");
    }
}
