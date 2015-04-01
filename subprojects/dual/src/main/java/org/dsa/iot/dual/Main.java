package org.dsa.iot.dual;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public void onRequesterConnected(DSLink link) {
        LOGGER.info("Requester link added");
        link.getRequester().list(new ListRequest("/"),
                new Handler<ListResponse>() {
            @Override
            public void handle(ListResponse event) {
                LOGGER.info("Request on root node complete");
                Map<String, Node> children = event.getNode().getChildren();
                if (children != null) {
                    for (String child : children.keySet()) {
                        LOGGER.info("Child node of root: " + child);
                    }
                }
            }
        });
    }

    public void onResponderConnected(DSLink link) {
        LOGGER.info("Responder link added");
        NodeBuilder builder = link.getNodeManager().createRootNode("values");
        Node node = builder.build();

        builder = node.createChild("string");
        builder.setValue(new Value("Hello world"));
        builder.build();
    }

    public static void main(String[] args) {
        DSLinkFactory.startDual("dual", args, new Main());
    }
}
