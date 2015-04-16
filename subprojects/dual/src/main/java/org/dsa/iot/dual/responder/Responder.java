package org.dsa.iot.dual.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Responder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Responder.class);

    /**
     * Initializes the responder link.
     *
     * @param link Responder link to initialize.
     */
    public static void init(DSLink link) {
        initSettableNode(link.getNodeManager());
    }

    /**
     * Initializes the 'settable' node. By default this node will have a value
     * of 'UNSET' until it is set by the requester.
     *
     * @param manager Node manager of the link.
     * @see org.dsa.iot.dual.requester.Requester#setNodeValue
     */
    private static void initSettableNode(NodeManager manager) {
        NodeBuilder builder = manager.createRootNode("values");
        Node node = builder.build();

        builder = node.createChild("settable");
        node = builder.build();
        node.setValue(new Value("UNSET"));
        LOGGER.info("Responder has a current value of {}", node.getValue().toString());
        node.getListener().addValueHandler(new Handler<Value>() {
            @Override
            public void handle(Value event) {
                String val = event.toString();
                LOGGER.info("Responder has a new value set from requester: {}", val);
            }
        });
    }
}
