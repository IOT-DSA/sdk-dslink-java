package org.dsa.iot.dslink.responder.methods;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.Subscription;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class SubscribeMethod extends Method {

    @Getter @NonNull private final Client client;
    @NonNull private final NodeManager manager;
    private Map<Node, Subscription> nodes;

    public SubscribeMethod(Client client,
                            NodeManager manager,
                            JsonObject request) {
        super(request);
        this.client = client;
        this.manager = manager;
    }

    @Override
    @SneakyThrows
    public JsonArray invoke() {
        nodes = getPaths(getRequest().getArray("paths"));
        for (Node n : nodes.keySet()) {
            val sub = new Subscription(client);
            n.subscribe(sub);
        }

        setState(StreamState.CLOSED);
        return null;
    }

    @Override
    public void postSent() {
        if (nodes != null) {
            for (Map.Entry<Node, Subscription> n : nodes.entrySet()) {
                val node = n.getKey();
                val sub = n.getValue();
                sub.update(node);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<Node, Subscription> getPaths(JsonArray array) {
        Map<Node, Subscription> subscriptions = new HashMap<>();
        for (String s : (List<String>) array.toList()) {
            val tuple = manager.getNode(s);
            if (tuple == null)
                throw new NoSuchPathException(s);
            subscriptions.put(tuple.getKey(), new Subscription(client));
        }
        return subscriptions;
    }
}
