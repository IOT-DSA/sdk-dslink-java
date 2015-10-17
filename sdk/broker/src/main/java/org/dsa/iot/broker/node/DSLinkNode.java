package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.utils.MessageProcessor;
import org.dsa.iot.broker.utils.ParsedPath;
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

    public void clientConnected(Client client) {
        String cDsId = client.handshake().dsId();
        if (!(this.dsId == null || this.dsId.equals(cDsId))) {
            throw new IllegalStateException("Expected different dsId");
        }
        this.dsId = cDsId;
        this.disconnected = null;
        this.processor = new MessageProcessor(this);
        this.client = client;
    }

    public void clientDisconnected() {
        this.disconnected = TimeUtils.format(System.currentTimeMillis());
        this.client = null;
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
    public JsonObject list(ParsedPath pp, Client requester, int rid) {
        if (pp.isRemote()) {
            processor().addListStream(pp, requester, rid);
        }
        return null;
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
