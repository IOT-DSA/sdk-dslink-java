package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.link.Requester;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class SubscriptionUpdate extends Response {

    private final Requester requester;
    private final NodeManager manager;

    public SubscriptionUpdate(Requester requester) {
        this.requester = requester;
        this.manager = requester.getDSLink().getNodeManager();
    }

    @Override
    public int getRid() {
        return 0;
    }

    @Override
    public void populate(JsonObject in) {
        JsonArray updates = in.get("updates");
        Map<Integer, String> paths = requester.getSubscriptionIDs();
        Map<Integer, Handler<SubscriptionValue>> handlers = requester.getSubscriptionHandlers();
        if (updates != null) {
            for (Object obj : updates) {
                int rid;
                String path;
                Object valueObj;
                String timestamp;
                Number count = null;
                Number sum = null;
                Number min = null;
                Number max = null;

                if (obj instanceof JsonArray) {
                    JsonArray update = (JsonArray) obj;
                    rid = update.get(0);
                    path = paths.get(rid);
                    valueObj = update.get(1);
                    timestamp = update.get(2);
                } else if (obj instanceof JsonObject) {
                    JsonObject update = (JsonObject) obj;
                    rid = update.get("sid");
                    path = paths.get(rid);
                    valueObj = update.get("value");
                    timestamp = update.get("ts");
                    count = update.get("count");
                    sum = update.get("sum");
                    min = update.get("min");
                    max = update.get("max");
                } else {
                    String err = "Invalid subscription update: " + in;
                    throw new RuntimeException(err);
                }
                if (path == null) {
                    continue;
                }

                final Node node = manager.getNode(path, true).getNode();
                Value val = ValueUtils.toValue(valueObj, timestamp);
                if (val == null) {
                    ValueType type = node.getValueType();
                    if (type != null) {
                        val = ValueUtils.toEmptyValue(type, timestamp);
                    } else {
                        continue;
                    }
                }

                Handler<SubscriptionValue> handler = handlers.get(rid);
                SubscriptionValue value;
                if (handler != null) {
                    value = new SubscriptionValue(path, val, count, sum, min, max);
                    handler.handle(value);
                }

                node.setValueType(val.getType());
                node.setValue(val);
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
