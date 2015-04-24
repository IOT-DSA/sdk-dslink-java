package org.dsa.iot.dual.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
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
        initActionNode(node);
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
        builder.getListener().addValueHandler(new Handler<Value>() {
            @Override
            public void handle(Value event) {
                String val = event.toString();
                LOGGER.info("Responder has a new value set from requester: {}", val);
            }
        });
        node = builder.build();
        node.setValue(new Value("UNSET"));
        LOGGER.info("Responder has a current value of {}", node.getValue().toString());
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

    /**
     * Initializes the 'action' node that can be invoked.
     *
     * @param node Values node.
     * @see org.dsa.iot.dual.requester.Requester#invoke
     */
    private static void initActionNode(Node node) {
        NodeBuilder builder = node.createChild("action");
        builder.setAction(new Action(Permission.READ, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                LOGGER.info("Responder action invoked from requester");
            }
        }));
        builder.build();
    }
}
