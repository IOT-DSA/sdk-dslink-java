package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.dslink.util.TimeUtils;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class DSLinkNode {

    private Client client;
    private String disconnected;
    private JsonObject linkData;

    public void setClient(Client client) {
        this.client = client;
        if (client == null) {
            disconnected = TimeUtils.format(System.currentTimeMillis());
        } else {
            disconnected = null;
        }
    }

    public Client getClient() {
        return client;
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
}
