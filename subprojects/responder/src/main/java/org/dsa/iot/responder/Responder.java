package org.dsa.iot.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;

/**
 * @author Samuel Grenier
 */
public class Responder extends DSLinkHandler {

    @Override
    public void onResponderConnected(DSLink link) {
        // Tests path subscriptions
        NodeManager manager = link.getNodeManager();
        Node parent = manager.createRootNode("replicator", "node");
        startThread(parent);
    }

    private void startThread(final Node node) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(1000);
                        node.createChild(String.valueOf(i), "node");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void main(String[] args) {
        DSLinkFactory.startResponder("responder", args, new Responder());
    }
}
