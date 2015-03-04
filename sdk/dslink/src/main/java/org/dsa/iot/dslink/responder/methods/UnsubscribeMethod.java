package org.dsa.iot.dslink.responder.methods;

import lombok.val;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class UnsubscribeMethod extends SubscribeMethod {
    
    public UnsubscribeMethod(Client client,
                                NodeManager manager,
                                JsonObject request) {
        super(client, manager, request);
    }

    @Override
    public JsonArray invoke() {
        val nodes = getPaths(getRequest().getArray("paths"));
        for (Node n : nodes.keySet()) {
            n.unsubscribe(getClient());
        }

        setState(StreamState.CLOSED);
        return null;
    }
}
