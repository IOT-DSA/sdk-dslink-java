package org.dsa.iot.dslink.node;

import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the node manager.
 * @author Samuel Grenier
 */
public class NodeManagerTest {

    /**
     * Tests node additions to the node manager. Also exercises node path
     * building when retrieving nodes.
     */
    @Test
    public void nodeAdditions() {
        NodeManager manager = new NodeManager();
        manager.createRootNode("A");

        Assert.assertNotNull(manager.getNode("A"));
        Assert.assertNotNull(manager.getNode("/A"));
        Assert.assertNotNull(manager.getNode("/A/"));
        Assert.assertNotNull(manager.getNode("/A//"));

        manager.createRootNode("A").createChild("B");
        Assert.assertNotNull(manager.getNode("/A/B"));
    }

    /**
     * Tests that node removals are completely out of the node manager.
     */
    @Test(expected = NoSuchPathException.class)
    public void nodeRemovals() {
        NodeManager manager = new NodeManager();
        manager.createRootNode("A");
        manager.getNode("/").removeChild("A");
        manager.getNode("/A");
    }

    @Test(expected = NullPointerException.class)
    public void nullPath() {
        NodeManager manager = new NodeManager();
        manager.getNode(null);
    }
}
