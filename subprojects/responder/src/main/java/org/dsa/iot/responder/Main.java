package org.dsa.iot.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Override
    public void onResponderInitialized(DSLink link) {
        NodeManager manager = link.getNodeManager();
        Node superRoot = manager.getSuperRoot();

        Replicator.start(superRoot);
        RNG.init(superRoot);
        Values.init(superRoot);
        LOGGER.info("Responder initialized");
    }

    public static void main(String[] args) {
        DSLinkFactory.startResponder("responder", args, new Main());
    }
}
