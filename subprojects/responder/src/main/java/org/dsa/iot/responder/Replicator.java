package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionRegistry;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.vertx.java.core.Handler;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a bunch of nodes to test children updates.
 * @author Samuel Grenier
 */
public class Replicator {

    private static final Map<Node, Replicator> NODES;

    private Node node;
    private Thread thread;

    private Replicator(Node node) {
        this.node = node;
    }

    public static void addActions(ActionRegistry registry) {
        Permission perm = Permission.READ;
        registry.register(new Action("reset", perm, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Node node = event.getNode().getParent();
                Replicator rep = NODES.get(node);
                rep.reset();
                rep.startThread();
            }
        }));
    }

    public static void start(Node parent) {
        Node node = parent.createChild("replicator").build();
        node.createChild("reset").setAction("reset").build();
        Replicator rep = new Replicator(node);
        NODES.put(node, rep);
        rep.startThread();
    }

    private synchronized void startThread() {
        reset();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(1000);
                        node.createChild(String.valueOf(i)).build();
                    }
                } catch (InterruptedException ignored) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private synchronized void reset() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
        for (int i = 0; i < 100; i++) {
            node.removeChild(String.valueOf(i));
        }
    }

    static {
        NODES = new HashMap<>();
    }
}
