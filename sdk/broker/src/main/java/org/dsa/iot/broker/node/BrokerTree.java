package org.dsa.iot.broker.node;

import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.client.Client;

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

    public BrokerNode getRoot() {
        return root;
    }

    public String init(String name, String dsId) {
        return downstream.init(name, dsId);
    }

    public void connected(Client client) {
        downstream.connected(client);
    }

    public void disconnected(Client client) {
        downstream.disconnected(client);
    }
}
