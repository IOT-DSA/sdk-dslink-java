package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.utils.MessageProcessor;
import org.dsa.iot.dslink.util.TimeUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class DSLinkNode extends BrokerNode {

    // The reserved dsId that can claim this node
    private String dsId;
    private Client client;

    private MessageProcessor processor;
    private String disconnected;
    private JsonObject linkData;

    public DSLinkNode(Downstream parent, String name) {
        super(parent, name, "dslink");
    }

    public void setClient(Client client) {
        if (client == null) {
            this.disconnected = TimeUtils.format(System.currentTimeMillis());
            this.client = null;
        } else {
            this.dsId = client.handshake().dsId();
            this.disconnected = null;
            this.processor = new MessageProcessor(this);
            this.client = client;
        }
    }

    public Client client() {
        return client;
    }

    public MessageProcessor processor() {
        return processor;
    }

    public String dsId() {
        return dsId;
    }

    public void linkData(JsonObject linkData) {
        this.linkData = linkData;
    }

    public JsonObject linkData() {
        return linkData;
    }

    public String disconnected() {
        return disconnected;
    }

    @Override
    protected void populateUpdates(JsonArray updates) {
        super.populateUpdates(updates);
        String disconnected = disconnected();
        if (disconnected != null) {
            JsonArray update = new JsonArray();
            update.add("disconnectedTs");
            update.add(disconnected);
        }
    }

    @Override
    protected JsonObject getChildUpdate() {
        JsonObject tmp = super.getChildUpdate();
        JsonObject linkData = linkData();
        if (linkData != null) {
            tmp.put("linkData", linkData);
        }
        return tmp;
    }
}
