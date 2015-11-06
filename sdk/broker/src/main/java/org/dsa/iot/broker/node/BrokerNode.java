package org.dsa.iot.broker.node;

import org.dsa.iot.broker.processor.stream.SubStream;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Samuel Grenier
 */
public class BrokerNode<T extends BrokerNode> {

    private final Map<String, T> children = new ConcurrentHashMap<>();
    private final WeakReference<BrokerNode> parent;
    private final String profile;
    private final String name;
    private final String path;
    private boolean accessible = true;

    private Map<Client, Integer> pathSubs;

    public BrokerNode(BrokerNode parent, String name, String profile) {
        if (profile == null || profile.isEmpty()) {
            throw new IllegalArgumentException("profile");
        }
        this.profile = profile;
        if (parent == null) {
            this.parent = null;
            this.name = null;
            this.path = "";
        } else {
            name = StringUtils.encodeName(name);
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name");
            }
            this.parent = new WeakReference<>(parent);
            this.name = name;
            this.path = parent.path + "/" + name;
        }
    }

    public void connected(Client client) {
    }

    public void disconnected(Client client) {
        if (pathSubs != null) {
            pathSubs.remove(client);
        }
    }

    public void propagateConnected(Client client) {
        for (BrokerNode child : children.values()) {
            child.connected(client);
            child.propagateConnected(client);
        }
    }

    public void propagateDisconnected(Client client) {
        for (BrokerNode child : children.values()) {
            child.disconnected(client);
            child.propagateDisconnected(client);
        }
    }

    public void accessible(boolean accessible) {
        boolean post = this.accessible != accessible;
        if (post && parent != null) {
            BrokerNode parent = this.parent.get();
            if (parent != null) {
                parent.childUpdate(this, !accessible);
            }
        }
        this.accessible = accessible;
    }

    public boolean accessible() {
        return accessible;
    }

    public String getName() {
        return name;
    }

    public boolean hasChild(String name) {
        return children.containsKey(name);
    }

    public void addChild(T child) {
        if (child == null) {
            return;
        }
        children.put(child.getName(), child);
        if (child.accessible()) {
            childUpdate(child, false);
        }
    }

    public T getChild(String name) {
        return children.get(name);
    }

    public SubStream subscribe(ParsedPath path, Client requester, int sid) {
        throw new UnsupportedOperationException();
    }

    public JsonObject list(ParsedPath path, Client requester, int rid) {
        if (pathSubs == null) {
            initializePathSubs();
        }
        pathSubs.put(requester, rid);

        JsonArray updates = new JsonArray();
        populateUpdates(updates);
        JsonObject obj = new JsonObject();
        obj.put("stream", StreamState.OPEN.getJsonName());
        obj.put("updates", updates);
        return obj;
    }

    protected void populateUpdates(JsonArray updates) {
        {
            JsonArray update = new JsonArray();
            update.add("$is");
            update.add(profile);
            updates.add(update);
        }
        {
            for (BrokerNode node : children.values()) {
                if (!node.accessible()) {
                    continue;
                }
                JsonArray update = new JsonArray();
                update.add(node.name);
                update.add(node.getChildUpdate());
                updates.add(update);
            }
        }
    }

    protected JsonObject getChildUpdate() {
        JsonObject obj = new JsonObject();
        obj.put("$is", profile);
        return obj;
    }

    protected void childUpdate(BrokerNode node, boolean removed) {
        if (node == null || pathSubs == null || pathSubs.size() <= 0) {
            return;
        }
        JsonArray updates = new JsonArray();
        if (removed) {
            JsonObject update = new JsonObject();
            update.put("name", node.getName());
            update.put("change", "remove");
            updates.add(update);
        } else {
            JsonArray update = new JsonArray();
            update.add(node.getName());
            update.add(node.getChildUpdate());
            updates.add(update);
        }

        JsonObject resp = new JsonObject();
        resp.put("stream", StreamState.OPEN.getJsonName());
        resp.put("updates", updates);

        JsonArray resps = new JsonArray();
        resps.add(resp);
        JsonObject top = new JsonObject();
        top.put("responses", resps);
        for (Map.Entry<Client, Integer> sub : pathSubs.entrySet()) {
            resp.put("rid", sub.getValue());
            sub.getKey().write(top.encode());
        }
    }

    private synchronized void initializePathSubs() {
        if (pathSubs == null) {
            pathSubs = new ConcurrentHashMap<>();
        }
    }
}
