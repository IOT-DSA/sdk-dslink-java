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

    public String initResponder(String name, String dsId) {
        synchronized (this) {
            DSLinkNode node = getChild(name);
            if (!(node == null || node.getDsId().equals(dsId))) {
                StringBuilder tmp = new StringBuilder(name);
                tmp.append("-");
                tmp.append(randomChar());

                while (hasChild(tmp.toString())) {
                    tmp.append(randomChar());
                }
                name = tmp.toString();
            }
            if (node == null) {
                node = new DSLinkNode(this, name);
                addChild(node);
            }
        }
        return name;
    }

    public void respConnected(Client client) {
        DSLinkNode node = getNode(client);
        node.setClient(client);
        node.setLinkData(client.handshake().linkData());
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
        if (!client.handshake().isResponder()) {
            throw new IllegalStateException("Client is not a responder");
        }
        return client.handshake().name();
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
