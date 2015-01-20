package org.dsa.iot.responder.methods;

import org.dsa.iot.responder.node.Node;
import org.dsa.iot.responder.node.NodeManager;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

/**
 * @author Samuel Grenier
 */
public class UnsubscribeMethod extends SubscribeMethod {

    public UnsubscribeMethod(NodeManager manager) {
        super(manager);
    }

    @Override
    public JsonObject invoke(JsonObject request) {
        List<Node> nodes = getPaths(request.getArray("paths"));
        for (Node n : nodes) {
            n.setSubscribed(false);
        }

        setState(StreamState.CLOSED);
        return null;
    }
}