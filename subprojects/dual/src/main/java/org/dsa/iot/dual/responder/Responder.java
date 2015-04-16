package org.dsa.iot.dual.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public class Responder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Responder.class);
    private static final Random RANDOM = new Random();

    /**
     * Initializes the responder link.
     *
     * @param link Responder link to initialize.
     */
    public static void init(DSLink link) {
        NodeBuilder builder = link.getNodeManager().createRootNode("values");
        Node node = builder.build();

        initSettableNode(node);
        initDynamicNode(node);
    }

    /**
     * Initializes the 'settable' node. By default this node will have a value
     * of 'UNSET' until it is set by the requester.
     *
     * @param node Values node.
     * @see org.dsa.iot.dual.requester.Requester#setNodeValue
     */
    private static void initSettableNode(Node node) {
        NodeBuilder builder = node.createChild("settable");
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

    /**
     * Initializes the 'dynamic' node which is an RNG. This RNG can be
     * subscribed to in order to show how subscriptions work.
     *
     * @param node Values node.
     * @see org.dsa.iot.dual.requester.Requester#subscribe
     */
    private static void initDynamicNode(Node node) {
        NodeBuilder builder = node.createChild("dynamic");
        final Node child = builder.build();

        Objects.getDaemonThreadPool().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                child.setValue(new Value(RANDOM.nextInt()));
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
