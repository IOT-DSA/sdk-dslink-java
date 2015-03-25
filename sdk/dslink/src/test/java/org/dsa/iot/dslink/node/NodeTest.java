package org.dsa.iot.dslink.node;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the node API.
 *
 * @author Samuel Grenier
 */
public class NodeTest {

    /**
     * Ensures the names are accurate
     */
    @Test
    public void nameTest() {
        Node node = new Node("Test", null, null);
        Assert.assertEquals("Test", node.getName());
        Assert.assertNotEquals("test", node.getName());
        Assert.assertNull(node.getDisplayName());
    }

    /**
     * Ensures that the node can build its node properly
     */
    @Test
    public void pathBuilding() {
        Node node = new Node("A", null, null);
        Assert.assertEquals("/A", node.getPath());

        node = node.createChild("A_B").build().createChild("B_A").build();
        Assert.assertEquals("/A/A_B/B_A", node.getPath());
    }

    /**
     * Ensures that configurations are null if none were set.
     */
    @Test
    public void noConfigs() {
        Node node = new Node("Test", null, null);
        Assert.assertNull(node.getConfigurations());
        Assert.assertNull(node.getConfig("nothing"));
    }

    /**
     * Ensures that attributes are null if none were set.
     */
    @Test
    public void noAttributes() {
        Node node = new Node("Test", null, null);
        Assert.assertNull(node.getAttributes());
        Assert.assertNull(node.getAttribute("nothing"));
    }
}
