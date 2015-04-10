package org.dsa.iot.dual;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Random RANDOM = new Random();

    private CountDownLatch latch;
    private String connName;
    private Node mirror;

    @Override
    public void preInit() {
        // Assumes no conflicting ID on broker
        LocalKeys keys = getConfig().getKeys();
        String dsId = getConfig().getDsId() + "-";
        dsId += keys.encodedHashPublicKey().charAt(0);
        connName = dsId;

        // Latch is used to ensure responder is initialized first
        latch = new CountDownLatch(1);
    }

    @Override
    public void onRequesterInitialized(final DSLink link) {
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

        // Generate value
        Value value = new Value(RANDOM.nextInt());

        // Update requester cache
        NodeManager manager = link.getNodeManager();
        String path = "/conns/" + connName + "/values/mirror";
        Node node = manager.getNode(path, true).getNode();
        node.setValue(value);

        // Update responder cache
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mirror.setValue(value);
        LOGGER.info("New mirror value: {}", value.toString());
    }

    @Override
    public void onResponderInitialized(DSLink link) {
        LOGGER.info("Responder link added");
        NodeBuilder builder = link.getNodeManager().createRootNode("values");
        final Node node = builder.build();

        builder = node.createChild("string");
        builder.setValue(new Value("Hello world"));
        builder.build();

        builder = node.createChild("mirror");
        builder.setValue(new Value(0));
        mirror = builder.build();
        LOGGER.info("Old mirror value: {}", mirror.getValue().toString());

        // Release the latch allowing the requester to update the value
        latch.countDown();
    }

    public static void main(String[] args) {
        DSLinkFactory.startDual("dual", args, new Main());
    }
}
