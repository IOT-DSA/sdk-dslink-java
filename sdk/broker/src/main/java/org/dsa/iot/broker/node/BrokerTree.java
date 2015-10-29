package org.dsa.iot.broker.node;

import org.dsa.iot.broker.server.client.Client;

/**
 * @author Samuel Grenier
 */
public class BrokerTree {

    private final BrokerNode<BrokerNode> root = new BrokerNode<>(null, null, "broker");
    private Downstream downstream;

    public void initialize(String downStreamName) {
        initialize(new Downstream(root, downStreamName));
    }

    public void initialize(Downstream downstream) {
        this.downstream = downstream;
        root.addChild(downstream);
    }

    public BrokerNode getRoot() {
        return root;
    }

    public String initDslink(String name, String dsId) {
        return downstream.init(name, dsId);
    }

    public void connected(Client client) {
        root.connected(client);
        root.propagateConnected(client);
    }

    public void disconnected(Client client) {
        root.disconnected(client);
        root.propagateDisconnected(client);
    }
}
