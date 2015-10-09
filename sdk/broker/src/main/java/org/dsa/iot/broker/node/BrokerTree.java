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
