package org.dsa.iot.broker.node;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class BrokerTree {

    private final BrokerNode<BrokerNode> root = new BrokerNode<>(null, null, "broker");
    private Downstream downstream;

    public void initialize(Broker broker) {
        String name = broker.getDownstreamName();
        this.downstream = new Downstream(root, name);
        root.addChild(downstream);
    }

    public JsonObject list(String path) {
        path = NodeManager.normalizePath(path, true);
        if (path.equals("/")) {
            return root.list();
        }

        BrokerNode<?> node = root;
        {
            String[] split = NodeManager.splitPath(path);
            if (split.length > 1 && split[0].equals(downstream.getName())) {
                // TODO: redirect to responder dslink
                node = null;
            }

            for (String name : split) {
                if (node == null) {
                    break;
                }
                node = node.getChild(name);
            }
        }
        return node == null ? null : node.list();
    }

    public void initResponder(Client client) {
        downstream.initResponder(client);
    }

    public void respConnected(Client client) {
        downstream.respConnected(client);
    }

    public void respDisconnected(Client client) {
        downstream.respDisconnected(client);
    }
}
