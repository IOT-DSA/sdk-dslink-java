package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Samuel Grenier
 */
public class NodeTree {

    private static final Random RANDOM = new Random();
    private static final String ALPHABET;

    private final Map<String, DSLinkNode> responders = new HashMap<>();

    public void initResponder(Client client) {
        if (!client.isResponder()) {
            throw new IllegalStateException("Client is not a responder");
        }
        StringBuilder name = new StringBuilder(client.getName());
        String path;
        synchronized (responders) {
            if (responders.containsKey(name.toString())) {
                name.append("-");
                name.append(randomChar());
            }
            while (responders.containsKey(name.toString())) {
                name.append(randomChar());
            }
            path = "/" + client.getBroker().getDownstreamName() + "/" + name;
            client.setPath(path);

            DSLinkNode node = new DSLinkNode();
            responders.put(path, node);
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
        String path = validateClient(client);
        DSLinkNode node;
        synchronized (responders) {
            node = responders.get(path);
            if (node == null) {
                throw new IllegalStateException("Client uninitialized");
            }
        }
        return node;
    }

    private static String validateClient(Client client) {
        String path;
        if (!client.isResponder()) {
            throw new IllegalStateException("Client is not a responder");
        } else if ((path = client.getPath()) == null) {
            throw new IllegalStateException("Client doesn't have path");
        }
        return path;
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
