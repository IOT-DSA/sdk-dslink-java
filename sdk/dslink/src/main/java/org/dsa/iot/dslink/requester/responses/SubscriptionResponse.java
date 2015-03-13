package org.dsa.iot.dslink.requester.responses;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.dsa.iot.core.Pair;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueStatus;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.requester.requests.SubscribeRequest;
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
    private String status = ValueStatus.OK.getJsonName();

    private Number count;
    private Number sum;
    private Number max;
    private Number min;

    private List<Node> nodeList = new ArrayList<>();

    public SubscriptionResponse(SubscribeRequest request, NodeManager manager) {
        super(request);
        this.manager = manager;
    }

    @Override
    public void populate(JsonArray array) {
        for (Object obj : array) {
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
                if (!ValueStatus.DISCONNECTED.getJsonName().equals(status)) {
                    value = ValueUtils.toValue(json.getField("value"));
                    count = json.getNumber("count");
                    sum = json.getNumber("sum");
                    max = json.getNumber("max");
                    min = json.getNumber("min");
                }
            } else {
                throw new RuntimeException("Unhandled update type");
            }

            Pair<Node, String> tuple = manager.getNode(path, true);
            Node node = tuple.getKey();
            String data = tuple.getValue();
            if (data != null) {
                if (data.startsWith("$")) { // configuration
                    data = data.substring(1);
                    node.setConfiguration(data, value);
                } else if (data.startsWith("@")) {// attribute
                    data = data.substring(1);
                    node.setAttribute(data, value);
                }
            } else {
                node.setValue(value);
            }

            nodeList.add(node);
        }
    }
}
