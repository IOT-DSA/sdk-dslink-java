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

    public String init(String name, String dsId) {
        synchronized (this) {
            DSLinkNode node = getChild(name);
            if (!(node == null || node.dsId().equals(dsId))) {
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
                node.accessible(false);
                addChild(node);
            }
        }
        return name;
    }

    @Override
    public void connected(Client client) {
        DSLinkNode node = getChild(client.handshake().name());
        node.connected(client);
    }

    @Override
    public void disconnected(Client client) {
        DSLinkNode node = getChild(client.handshake().name());
        node.disconnected(client);
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
