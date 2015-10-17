package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * @author Samuel Grenier
 */
public class Downstream extends BrokerNode<DSLinkNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downstream.class);
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

    public void connected(Client client) {
        LOGGER.info("Client `{}` has connected", client.handshake().dsId());
        DSLinkNode node = getNode(client);
        node.clientConnected(client);
        if (client.handshake().isResponder()) {
            node.linkData(client.handshake().linkData());
            node.accessible(true);
        } else {
            node.linkData(null);
            node.accessible(false);
        }
        client.node(node);
    }

    public void disconnected(Client client) {
        DSLinkNode node = getNode(client);
        node.clientDisconnected();
        client.node(null);
        LOGGER.info("Client `{}` has disconnected", client.handshake().dsId());
    }

    private DSLinkNode getNode(Client client) {
        String name = client.handshake().name();
        DSLinkNode node;
        synchronized (this) {
            node = getChild(name);
            if (node == null) {
                throw new IllegalStateException("Client uninitialized");
            }
        }
        return node;
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
