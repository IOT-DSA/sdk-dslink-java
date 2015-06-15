package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class SubscriptionUpdate implements Response {

    private final Requester requester;
    private final NodeManager manager;
    private final Map<String, SubscriptionValue> updates = new HashMap<>();

    public SubscriptionUpdate(Requester requester) {
        this.requester = requester;
        this.manager = requester.getDSLink().getNodeManager();
    }

    @Override
    public int getRid() {
        return 0;
    }

    public Map<String, SubscriptionValue> getUpdates() {
        return updates;
    }

    @Override
    public void populate(JsonObject in) {
        JsonArray updates = in.getArray("updates");
        Map<Integer, String> paths = requester.getSubscriptionIDs();
        Map<Integer, Handler<SubscriptionValue>> handlers = requester.getSubscriptionHandlers();
        if (updates != null) {
            for (Object obj : updates) {
                int rid;
                String path;
                SubscriptionValue value;
                if (obj instanceof JsonArray) {
                    JsonArray update = (JsonArray) obj;
                    rid = update.get(0);
                    path = paths.get(rid);
                    Object o = update.get(1);
                    String ts = update.get(2);
                    Value val = null;
                    if (o != null) {
                        val = ValueUtils.toValue(o);
                    }
                    value = new SubscriptionValue(path, val, ts);
                    this.updates.put(path, value);
                } else if (obj instanceof JsonObject) {
                    JsonObject update = (JsonObject) obj;
                    rid = update.getInteger("sid");
                    path = paths.get(rid);
                    Object o = update.getField("value");
                    Value val = null;
                    if (o != null) {
                        val = ValueUtils.toValue(o);
                    }
                    Integer c = update.getInteger("count");
                    Integer s = update.getInteger("sum");
                    Integer min = update.getInteger("min");
                    Integer max = update.getInteger("max");
                    String ts = update.getString("ts");
                    value = new SubscriptionValue(path, val, ts, c, s, min, max);
                    this.updates.put(path, value);
                } else {
                    String err = "Invalid subscription update: " + in.encode();
                    throw new RuntimeException(err);
                }
                Node node = manager.getNode(path, true).getNode();
                {
                    Value val = value.getValue();
                    ValueType type = node.getValueType();
                    if (type == null && val != null) {
                        type = val.getType();
                        node.setValueType(type);
                    }
                    if (type != null) {
                        node.setValue(val);
                    }
                }
                Handler<SubscriptionValue> handler = handlers.get(rid);
                if (handler != null) {
                    handler.handle(value);
                }
            }
        }
    }

    @Override
    public JsonObject getJsonResponse(JsonObject in) {
        return null;
    }

    @Override
    public JsonObject getCloseResponse() {
        return null;
    }
}
