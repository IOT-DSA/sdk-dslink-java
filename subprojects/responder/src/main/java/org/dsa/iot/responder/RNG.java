package org.dsa.iot.responder;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * Creates a random number generator
 * @author Samuel Grenier
 */
public class RNG {

    private static final Logger LOGGER = LoggerFactory.getLogger(RNG.class);

    public void start(Node parent) {
        NodeBuilder builder = parent.createChild("rng");
        builder.setAttribute("count", new Value(0));
        Node child = builder.build();

        builder = child.createChild("addRNG");
        builder.setAction("addRNG");
        builder.build();
    }

    public static Handler<ActionResult> getAddHandler() {
        return new Handler<ActionResult>() {

            @Override
            public void handle(ActionResult event) {
                int incrementCount = 1;

                JsonObject params = event.getJsonIn().getObject("params");
                if (params != null) {
                    Integer count = params.getInteger("count");
                    if (count != null) {
                        incrementCount = count;
                    }
                }

                Node node = event.getNode().getParent();
                Value value = node.getAttribute("count");
                int min = value.getNumber().intValue();
                int max = min + incrementCount;
                node.setAttribute("count", new Value(max));

                for (int i = min; i < max; i++) {
                    NodeBuilder builder = node.createChild("rng_" + i)
                            .setValue(new Value(0));
                    Node child = builder.build();
                    LOGGER.info("Created RNG child at " + child.getPath());

                    builder = child.createChild("remove");
                    builder.setAction("removeRNG");
                    builder.build();

                    // TODO: dynamically changing numbers
                }
            }
        };
    }

    public static Handler<ActionResult> getRemoveHandler() {
        return new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Node parent = event.getNode().getParent();
                if (parent != null) {
                    Node child = event.getNode().getParent();
                    parent.removeChild(child);
                    LOGGER.info("Removed RNG child at " + child.getPath());
                }
            }
        };
    }
}
