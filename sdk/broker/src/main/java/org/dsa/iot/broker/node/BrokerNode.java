package org.dsa.iot.broker.node;

import org.dsa.iot.broker.processor.stream.GenericStream;
import org.dsa.iot.broker.processor.stream.SubStream;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
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

    private SubStream subStream;
    private ValueType type;
    private Value value;

    public BrokerNode(BrokerNode parent, String name) {
        this(parent, name, "node");
    }

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

    public void setValueType(ValueType type) {
        this.type = type;
    }

    public void setValue(Value value) {
        this.value = value;
        SubStream subs = this.subStream;
        if (subs == null) {
            return;
        }

        JsonArray update = generateValueUpdate();
        subs.dispatch(update);
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

    public String name() {
        return name;
    }

    public String path() {
        return path;
    }

    public boolean hasChild(String name) {
        return children.containsKey(name);
    }

    public void addChild(T child) {
        if (child == null) {
            return;
        }
        children.put(child.name(), child);
        if (child.accessible()) {
            childUpdate(child, false);
        }
    }

    public T getChild(String name) {
        return children.get(name);
    }

    public SubStream subscribe(ParsedPath path, Client requester, int sid) {
        if (subStream == null) {
            initializeValueSubs();
        }
        subStream.add(requester, sid);
        return subStream;
    }

    public void unsubscribe(SubStream stream, Client requester) {
        stream.remove(requester);
    }

    public GenericStream set(ParsedPath path,
                             Client requester,
                             int rid,
                             Object value,
                             String permit) {
        throw new UnsupportedOperationException();
    }

    public GenericStream remove(ParsedPath path,
                                Client requester,
                                int rid,
                                String permit) {
        throw new UnsupportedOperationException();
    }

    public GenericStream invoke(ParsedPath path,
                                Client requester,
                                int rid,
                                JsonObject params,
                                String permit) {
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
        ValueType type = this.type;
        if (type != null) {
            JsonArray update = new JsonArray();
            update.add("$type");
            update.add(type.toJsonString());
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

        ValueType type = this.type;
        if (type != null) {
            obj.put("$type", type.toJsonString());
        }

        return obj;
    }

    protected void childUpdate(BrokerNode node, boolean removed) {
        if (node == null || pathSubs == null || pathSubs.size() <= 0) {
            return;
        }
        JsonArray updates = new JsonArray();
        if (removed) {
            JsonObject update = new JsonObject();
            update.put("name", node.name());
            update.put("change", "remove");
            updates.add(update);
        } else {
            JsonArray update = new JsonArray();
            update.add(node.name());
            update.add(node.getChildUpdate());
            updates.add(update);
        }

        JsonObject resp = new JsonObject();
        resp.put("stream", StreamState.OPEN.getJsonName());
        resp.put("updates", updates);

        JsonArray resps = new JsonArray();
        resps.add(resp);
        for (Map.Entry<Client, Integer> sub : pathSubs.entrySet()) {
            resp.put("rid", sub.getValue());
            sub.getKey().writeResponse(resps);
        }
    }

    protected JsonArray generateValueUpdate() {
        JsonArray update = new JsonArray();
        update.add(null); // sid goes here
        if (value != null) {
            update.add(value);
            update.add(value.getTimeStamp());
        } else {
            update.add(null);
        }
        return update;
    }

    private synchronized void initializePathSubs() {
        if (pathSubs == null) {
            pathSubs = new ConcurrentHashMap<>();
        }
    }

    private synchronized void initializeValueSubs() {
        if (subStream == null) {
            ParsedPath pp = ParsedPath.parse(null, path);
            subStream = new SubStream(pp, this);
            subStream.dispatch(generateValueUpdate());
        }
    }
}
