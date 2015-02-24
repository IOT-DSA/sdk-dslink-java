package org.dsa.iot.dslink.responder.methods;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

/**
 * @author Samuel Grenier
 */
public class UnsubscribeMethod extends SubscribeMethod {

    public UnsubscribeMethod(NodeManager manager,
                                JsonObject request) {
        super(manager, request);
    }

    @Override
    public JsonArray invoke() {
        List<Node> nodes = getPaths(getRequest().getArray("paths"));
        for (Node n : nodes) {
            n.setSubscribed(false);
        }

        setState(StreamState.CLOSED);
        return null;
    }
}
