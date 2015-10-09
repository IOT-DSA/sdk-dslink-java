package org.dsa.iot.broker.node;

import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
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
    private final Map<String, Value> nodeOpts = new ConcurrentHashMap<>();
    private final WeakReference<BrokerNode> parent;
    private final String profile;
    private final String name;
    private final String path;

    public BrokerNode(BrokerNode parent, String name, String profile) {
        if (profile == null || profile.isEmpty()) {
            throw new IllegalArgumentException("profile");
        }
        this.profile = profile;
        if (parent == null) {
            this.name = null;
            this.path = "/";
            this.parent = null;
        } else {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name");
            }
            this.name = StringUtils.encodeName(name);
            this.path = "/" + parent.path;
            this.parent = new WeakReference<>(parent);
        }
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
        // TODO: send update to list subscriptions
        children.put(child.getName(), child);
    }

    public T getChild(String name) {
        return children.get(name);
    }

    public void setNodeOption(String key, Value value) {
        // TODO: send update to list subscriptions
        if (value == null) {
            nodeOpts.remove(key);
            return;
        }
        nodeOpts.put(key, value);
    }

    public JsonObject list() {
        JsonArray updates = new JsonArray();
        populateUpdates(updates);
        JsonObject obj = new JsonObject();
        // TODO: handle list subscriptions
        obj.put("stream", StreamState.CLOSED.getJsonName());
        obj.put("updates", updates);
        return obj;
    }

    protected void populateUpdates(JsonArray updates) {
        {
            JsonArray update = new JsonArray();
            update.add("$is");
            update.add(profile);
        }
        {
            for (Map.Entry<String, Value> entry : nodeOpts.entrySet()) {
                JsonArray update = new JsonArray();
                update.add(entry.getKey());
                ValueUtils.toJson(update, entry.getValue());
                updates.add(update);
            }
        }
        {
            for (BrokerNode node : children.values()) {
                JsonArray update = new JsonArray();
                update.add(node.name);
                {
                    JsonObject obj = new JsonObject();
                    obj.put("$is", node.profile);
                    update.add(obj);
                }
                updates.add(update);
            }
        }
    }
}
