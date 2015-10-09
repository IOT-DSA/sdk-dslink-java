package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.dslink.util.TimeUtils;
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
        this.client = client;
        if (client == null) {
            disconnected = TimeUtils.format(System.currentTimeMillis());
        } else {
            this.dsId = client.getDsId();
            disconnected = null;
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
}
