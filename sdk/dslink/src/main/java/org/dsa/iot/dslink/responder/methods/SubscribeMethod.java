package org.dsa.iot.dslink.responder.methods;

import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Samuel Grenier
 */
public class SubscribeMethod extends Method {

    @NonNull private final NodeManager manager;
    private List<Node> nodes;

    public SubscribeMethod(NodeManager manager,
                            JsonObject request) {
        super(request);
        this.manager = manager;
    }

    @Override
    public JsonArray invoke() {
        nodes = getPaths(getRequest().getArray("paths"));
        for (Node n : nodes) {
            n.setSubscribed(true);
        }

        setState(StreamState.CLOSED);
        return null;
    }
    
    @Override
    public void postSent() {
        val subs = manager.getSubManager();
        subs.update(nodes);
    }

    @SuppressWarnings("unchecked")
    protected List<Node> getPaths(JsonArray array) {
        List<Node> subscriptions = new ArrayList<>();
        for (String s : (List<String>) array.toList()) {
            NodeManager.NodeStringTuple tuple = manager.getNode(s);
            if (tuple == null)
                throw new NoSuchPathException(s);
            subscriptions.add(tuple.getNode());
        }
        return subscriptions;
    }
}
