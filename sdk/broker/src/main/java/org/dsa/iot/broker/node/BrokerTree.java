package org.dsa.iot.broker.node;

import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;

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

    public BrokerNode getNode(ParsedPath path) {
        BrokerNode<?> node = getRoot();
        {
            String[] split = path.split();
            for (String name : split) {
                BrokerNode tmp = node.getChild(name);
                if (tmp == null) {
                    if (!path.isRemote()) {
                        node = null;
                    }
                    break;
                }
                node = tmp;
            }
        }
        return node != null && node.accessible() ? node : null;
    }

    public String initDslink(String name, String dsId) {
        return downstream.init(name, dsId);
    }

    public void connected(Client client) {
        root.connected(client);
        root.propagateConnected(client);
        client.node(downstream.getChild(client.handshake().name()));
    }

    public void disconnected(Client client) {
        root.disconnected(client);
        root.propagateDisconnected(client);
        client.node(null);
    }
}
