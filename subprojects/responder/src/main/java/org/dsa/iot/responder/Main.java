package org.dsa.iot.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.actions.ActionRegistry;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    @Override
    public void preInit() {
        ActionRegistry registry = getActionRegistry();
        Replicator.addActions(registry);
        RNG.addActions(registry);
    }

    @Override
    public void onResponderConnected(DSLink link) {
        NodeManager manager = link.getNodeManager();
        Node superRoot = manager.getNode("/").getNode();

        Replicator.start(superRoot);
        RNG.init(superRoot);
        Values.init(superRoot);
    }

    public static void main(String[] args) {
        DSLinkFactory.startResponder("responder", args, new Main());
    }
}
