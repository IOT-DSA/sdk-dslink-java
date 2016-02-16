package org.dsa.iot.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.responder.rng.RNG;
import org.dsa.iot.responder.rng.TableActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Override
    public JsonObject getLinkData() {
        JsonObject obj = new JsonObject();
        obj.put("exampleResponder", true);
        return obj;
    }

    @Override
    public boolean isResponder() {
        return true;
    }

    @Override
    public void preInit() {
        String path = getWorkingDir().getAbsolutePath();
        LOGGER.info("Current working directory: {}", path);
    }

    @Override
    public void onResponderInitialized(DSLink link) {
        NodeManager manager = link.getNodeManager();
        Node superRoot = manager.getSuperRoot();

        Replicator.start(superRoot);
        RNG.init(superRoot);
        Values.init(superRoot);
        Echo.init(superRoot);
        TableActions.init(superRoot);
        LOGGER.info("Responder initialized");
    }

    @Override
    public void onResponderConnected(DSLink link) {
        LOGGER.info("Responder connected");
    }

    @Override
    public void onResponderDisconnected(DSLink link) {
        LOGGER.info("Oh no! The connected to the broker was lost");
    }

    public static void main(String[] args) {
        DSLinkFactory.start(args, new Main());
    }
}
