package org.dsa.iot.dslink.methods;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.exceptions.NoSuchPathException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Samuel Grenier
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class SubscribeMethod extends Method {

    @NonNull
    private final NodeManager manager;

    @Override
    public JsonArray invoke(JsonObject request) {
        List<Node> nodes = getPaths(request.getArray("paths"));
        for (Node n : nodes) {
            n.setSubscribed(true);
        }

        setState(StreamState.CLOSED);
        return null;
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
