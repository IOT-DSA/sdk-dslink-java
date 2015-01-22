package org.dsa.iot.dslink.methods;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
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