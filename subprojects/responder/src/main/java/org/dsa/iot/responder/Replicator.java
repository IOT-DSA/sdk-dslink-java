package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.vertx.java.core.Handler;

/**
 * Creates a bunch of nodes to test children updates.
 * @author Samuel Grenier
 */
public class Replicator {

    private Node node;
    private Thread thread;

    private Replicator(Node node) {
        this.node = node;
    }

    public static void start(Node parent) {
        Node node = parent.createChild("replicator").build();

        final Permission perm = Permission.READ;
        final Replicator rep = new Replicator(node);
        final Action act = new Action(perm, new ResetHandler(rep));
        node.createChild("reset").build().setAction(act);
        rep.startThread();
    }

    private synchronized void startThread() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 5; i++) {
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

    private static class ResetHandler implements Handler<ActionResult> {

        private final Replicator rep;

        public ResetHandler(Replicator rep) {
            this.rep = rep;
        }

        @Override
        public void handle(ActionResult event) {
            rep.reset();
            rep.startThread();
        }
    }
}
