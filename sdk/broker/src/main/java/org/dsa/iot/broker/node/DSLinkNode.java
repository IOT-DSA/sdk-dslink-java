package org.dsa.iot.broker.node;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.processor.MessageProcessor;
import org.dsa.iot.broker.utils.ParsedPath;
import org.dsa.iot.dslink.util.TimeUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class DSLinkNode extends BrokerNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DSLinkNode.class);

    // The reserved dsId that can claim this node
    private String dsId;
    private Client client;

    private MessageProcessor processor;
    private String disconnected;
    private JsonObject linkData;

    public DSLinkNode(Downstream parent, String name) {
        super(parent, name, "dslink");
    }

    @Override
    public void connected(Client client) {
        String cDsId = client.handshake().dsId();
        if (!(this.dsId == null || this.dsId.equals(cDsId))) {
            throw new IllegalStateException("Expected different dsId");
        }
        LOGGER.info("Client `{}` has connected", cDsId);
        this.dsId = cDsId;
        this.client = client;
        this.disconnected = null;
        this.processor = new MessageProcessor(this);
        client.node(this);
        if (client.handshake().isResponder()) {
            linkData(client.handshake().linkData());
            accessible(true);
        } else {
            linkData(null);
            accessible(false);
        }
    }

    @Override
    public void disconnected(Client client) {
        if (client == this.client) {
            this.disconnected = TimeUtils.format(System.currentTimeMillis());
            this.client = null;
            client.node(null);
            LOGGER.info("Client `{}` has disconnected", client.handshake().dsId());
        } else {
            if (client() != null && client().handshake().isResponder()
                    && client.handshake().isRequester()) {
                processor().responder().requesterDisconnected(client);
            }
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
    public JsonObject list(ParsedPath pp, Client requester, int rid) {
        if (pp.isRemote()) {
            processor().responder().addListStream(pp, requester, rid);
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
            updates.add(update);
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
