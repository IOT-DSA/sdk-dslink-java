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

    public String addOfflineDSLink(Client client) {
        if (!client.isResponder()) {
            return null;
        }
        StringBuilder name = new StringBuilder(client.getName());
        synchronized (responders) {
            if (responders.containsKey(name.toString())) {
                name.append("-");
                name.append(randomChar());
            }
            while (responders.containsKey(name.toString())) {
                name.append(randomChar());
            }
            DSLinkNode node = new DSLinkNode();
            responders.put(name.toString(), node);
        }
        return "/downstream/" + name;
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
