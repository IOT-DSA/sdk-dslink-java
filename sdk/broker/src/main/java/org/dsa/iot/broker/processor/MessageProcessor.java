package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.server.DsaHandshake;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class MessageProcessor {

    private final Responder responder;
    private final Requester requester;

    public MessageProcessor(DSLinkNode node) {
        DsaHandshake handshake = node.client().handshake();
        if (handshake.isResponder()) {
            this.responder = new Responder(node);
        } else {
            this.responder = null;
        }
        if (handshake.isRequester()) {
            this.requester = new Requester(node);
        } else {
            this.requester = null;
        }
    }

    public void processData(JsonObject data) {
        if (requester != null) {
            processRequests((JsonArray) data.get("requests"));
        }
        if (responder != null) {
            processResponses((JsonArray) data.get("responses"));
        }
    }

    public Responder responder() {
        return responder;
    }

    public Requester requester() {
        return requester;
    }

    protected void processRequests(JsonArray requests) {
        if (requests != null) {
            for (Object obj : requests) {
                requester.processRequest((JsonObject) obj);
            }
        }
    }

    protected void processResponses(JsonArray responses) {
        if (responses != null) {
            for (Object obj : responses) {
                responder.processResponse((JsonObject) obj);
            }
        }
    }
}
