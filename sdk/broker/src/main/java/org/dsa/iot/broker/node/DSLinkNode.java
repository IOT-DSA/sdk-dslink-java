package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
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
            this.client = client;
        }
    }

    public Client getClient() {
        return client;
    }

    public String getDsId() {
        return dsId;
    }

    public void setLinkData(JsonObject linkData) {
        this.linkData = linkData;
    }

    public JsonObject getLinkData() {
        return linkData;
    }

    public String getDisconnected() {
        return disconnected;
    }

    public int nextRid() {
        return client.nextRid();
    }

    @Override
    protected void populateUpdates(JsonArray updates) {
        super.populateUpdates(updates);
        String disconnected = getDisconnected();
        if (disconnected != null) {
            JsonArray update = new JsonArray();
            update.add("disconnectedTs");
            update.add(disconnected);
        }
    }

    @Override
    protected JsonObject getChildUpdate() {
        JsonObject tmp = super.getChildUpdate();
        JsonObject linkData = getLinkData();
        if (linkData != null) {
            tmp.put("linkData", linkData);
        }
        return tmp;
    }
}
