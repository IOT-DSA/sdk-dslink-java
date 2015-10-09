package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;

import java.util.Random;

/**
 * @author Samuel Grenier
 */
public class Downstream extends BrokerNode<DSLinkNode> {

    private static final Random RANDOM = new Random();
    private static final String ALPHABET;

    public Downstream(BrokerNode parent, String name) {
        super(parent, name, "node");
    }

    public void initResponder(Client client) {
        if (!client.isResponder()) {
            throw new IllegalStateException("Client is not a responder");
        }
        String name = client.getName();
        synchronized (this) {
            DSLinkNode node = getChild(name);
            if (!(node == null || node.getDsId().equals(client.getDsId()))) {
                StringBuilder tmp = new StringBuilder(name);
                tmp.append("-");
                tmp.append(randomChar());

                while (hasChild(tmp.toString())) {
                    tmp.append(randomChar());
                }
                name = tmp.toString();
            }
            client.setDownstreamName(name);
            if (node == null) {
                node = new DSLinkNode(this, name);
                addChild(node);
            }
        }
    }

    public void respConnected(Client client) {
        DSLinkNode node = getNode(client);
        node.setClient(client);
        node.setLinkData(client.getLinkData());
    }

    public void respDisconnected(Client client) {
        DSLinkNode node = getNode(client);
        node.setClient(null);
    }

    private DSLinkNode getNode(Client client) {
        String name = validateClient(client);
        DSLinkNode node;
        synchronized (this) {
            node = getChild(name);
            if (node == null) {
                throw new IllegalStateException("Client uninitialized");
            }
        }
        return node;
    }

    private static String validateClient(Client client) {
        String name;
        if (!client.isResponder()) {
            throw new IllegalStateException("Client is not a responder");
        } else if ((name = client.getDownstreamName()) == null) {
            throw new IllegalStateException("Client doesn't have path");
        }
        return name;
    }

    private static char randomChar() {
        return ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
    }

    static {
        String tmp = "";
        tmp += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        tmp += "abcdefghijklmnopqrstuvwxyz";
        tmp += "0123456789";
        ALPHABET = tmp;
    }
}
