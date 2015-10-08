package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

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
            path = client.getBroker().getDownstreamPath() + "/" + name;
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

    public void list(Client client, JsonObject obj) {
        String path = obj.get("path");
        path = NodeManager.normalizePath(path, true);
        if (path.equals("/")) {
            JsonArray updates = new JsonArray();
            {
                JsonArray update = new JsonArray();
                update.add("$is");
                update.add("broker");
                updates.add(update);
            }
            {
                JsonArray update = new JsonArray();
                update.add("$downstream");
                update.add(client.getBroker().getDownstreamPath());
                updates.add(update);
            }
            {
                JsonArray update = new JsonArray();
                update.add(client.getBroker().getDownstreamName());
                JsonObject o = new JsonObject();
                o.put("$is", "node");
                update.add(o);
                updates.add(update);
            }
            JsonObject resp = new JsonObject();
            resp.put("rid", obj.get("rid"));
            resp.put("stream", StreamState.CLOSED.getJsonName()); // TODO: list streams
            resp.put("updates", updates);

            JsonObject top = new JsonObject();
            {
                JsonArray resps = new JsonArray();
                resps.add(resp);
                top.put("responses", resps);
            }
            client.write(top.encode());
        }
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
