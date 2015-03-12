package org.dsa.iot.dslink;

import lombok.SneakyThrows;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.exceptions.DuplicateException;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.dsa.iot.dslink.responder.action.ActionRegistry;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class NodeTest {

    private final MBassador<Event> bus = EventBusFactory.create();
    private final ActionRegistry registry = new ActionRegistry();

    @Test
    @SneakyThrows
    public void nodeAdditions() {
        NodeManager manager = new NodeManager(bus, registry);
        Node nodeA = manager.createRootNode("A");

        Assert.assertNotNull(manager.getNode("A"));
        Assert.assertNotNull(manager.getNode("/A"));
        Assert.assertNotNull(manager.getNode("/A/"));
        Assert.assertNotNull(manager.getNode("/A//"));

        boolean noPath = false;
        try {
            manager.getNode("/A/A_A");
        } catch (NoSuchPathException e) {
            noPath = true;
        } finally {
            Assert.assertTrue(noPath);
        }
        manager.createRootNode("B");

        Assert.assertNotNull(manager.getNode("B"));
        Assert.assertNotNull(manager.getNode("/B"));
        Assert.assertNotNull(manager.getNode("/B/"));
        Assert.assertNotNull(manager.getNode("/B//"));

        try {
            noPath = false;
            manager.getNode("/A/B");
        } catch (NoSuchPathException e) {
            noPath = true;
        } finally {
            Assert.assertTrue(noPath);
        }

        try {
            noPath = false;
            manager.getNode("/B/A");
        } catch (NoSuchPathException e) {
            noPath = true;
        } finally {
            Assert.assertTrue(noPath);
        }

        Assert.assertNull(nodeA.getChildren());
        nodeA.createChild("A");
        Assert.assertNotNull(nodeA.getChildren());

        Assert.assertNotNull(manager.getNode("A/A"));
        Assert.assertNotNull(manager.getNode("/A/A"));
        Assert.assertNotNull(manager.getNode("/A/A/"));
        Assert.assertNotNull(nodeA.getChild("A"));
        Assert.assertNull(nodeA.getChild("/A"));
    }

    @Test
    @SneakyThrows
    public void nodeRemovals() {
        NodeManager manager = new NodeManager(bus, registry);
        Node a = manager.createRootNode("A");

        a.createChild("A_A");
        a.createChild("A_B");

        a.removeChild("A_A");
        a.removeChild(new Node(bus, null, "A_B", registry));

        Assert.assertNotNull(manager.getNode("/A"));

        boolean noPath = false;

        try {
            manager.getNode("/A/A_A");
        } catch (NoSuchPathException e) {
            noPath = true;
        } finally {
            Assert.assertTrue(noPath);
        }

        try {
            noPath = false;
            manager.getNode("/A/A_A");
        } catch (NoSuchPathException e) {
            noPath = true;
        } finally {
            Assert.assertTrue(noPath);
        }

        Assert.assertNotNull(a.getChildren());
        Assert.assertTrue(manager.getChildren("A").isEmpty());
        Assert.assertTrue(a.getChildren().isEmpty());
    }

    @Test
    @SneakyThrows
    public void children() {
        NodeManager manager = new NodeManager(bus, registry);
        Node a = manager.createRootNode("A");
        a.createChild("A_A");
        a.createChild("A_B");

        Assert.assertNotNull(manager.getChildren("A"));
        Assert.assertNotNull(manager.getChildren("/A"));
        Assert.assertNotNull(manager.getChildren("/A/"));

        Assert.assertEquals(2, manager.getChildren("A").size());
        Assert.assertEquals(2, a.getChildren().size());
    }

    @Test
    @SneakyThrows
    public void pathBuilding() {
        Node node = new Node(bus, null, "A", registry);
        node = node.createChild("A_B").createChild("B_A");
        Assert.assertEquals("/A/A_B/B_A", node.getPath());
    }

    @SneakyThrows
    @Test(expected = DuplicateException.class)
    public void duplicateRootNodes() {
        NodeManager manager = new NodeManager(bus, registry);
        manager.createRootNode("A");
        manager.createRootNode("A");
    }

    @Test
    public void illegalPathInput() {
        NodeManager manager = new NodeManager(bus, registry);

        boolean emptyPath = false;
        boolean nullPath = false;

        try {
            manager.getNode("");
        } catch (IllegalArgumentException e) {
            emptyPath = true;
        }

        try {
            manager.getNode(null);
        } catch (IllegalArgumentException e) {
            nullPath = true;
        }

        Assert.assertTrue(emptyPath);
        Assert.assertTrue(nullPath);
    }

    @Test(expected = NoSuchPathException.class)
    public void noSuchPath() {
        new NodeManager(bus, registry).getChildren("nothing");
    }
}
