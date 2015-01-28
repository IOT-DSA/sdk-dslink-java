package org.dsa.iot.dslink.responses;

import lombok.Getter;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requests.SubscribeRequest;
import org.dsa.iot.dslink.util.ValueStatus;
import org.dsa.iot.dslink.util.ValueUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
public class SubscriptionResponse extends Response<SubscribeRequest> {

    private final NodeManager manager;

    private String path;
    private Value value;
    private String timeStamp;
    private String status = ValueStatus.OK.jsonName;

    private Number count;
    private Number sum;
    private Number max;
    private Number min;

    public SubscriptionResponse(SubscribeRequest request,
                                NodeManager manager) {
        super(request);
        this.manager = manager;
    }

    @Override
    public void populate(JsonArray o) {
        for (Object obj : o) {
            if (obj instanceof JsonArray) {
                JsonArray json = (JsonArray) obj;
                path = json.get(0);
                value = ValueUtils.toValue(json.get(1));
                timeStamp = json.get(2);
            } else if (obj instanceof JsonObject) {
                JsonObject json = (JsonObject) obj;
                path = json.getString("path");
                status = json.getString("status");
                timeStamp = json.getString("ts");
                if (!ValueStatus.DISCONNECTED.jsonName.equals(status)) {
                    value = ValueUtils.toValue(json.getField("value"));
                    count = json.getNumber("count");
                    sum = json.getNumber("sum");
                    max = json.getNumber("max");
                    min = json.getNumber("min");
                }
            } else {
                throw new RuntimeException("Unhandled update type");
            }

            NodeManager.NodeStringTuple tuple = manager.getNode(path, true);
            Node node = tuple.getNode();
            String data = tuple.getString();
            if (data != null) {
                if (data.startsWith("$")) { // configuration
                    data = data.substring(1);
                    node.setConfiguration(data, value);
                } else if (data.startsWith("@")) {// attribute
                    data = data.substring(1);
                    node.setAttribute(data, value);
                }
            } else {
                tuple.getNode().setValue(value);
            }
        }
    }
}
