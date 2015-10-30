package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.server.DsaHandshake;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class MessageProcessor {

    private Responder responder;
    private Requester requester;

    public void initialize(DSLinkNode node) {
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
        Requester requester = this.requester;
        if (requester != null) {
            processRequests((JsonArray) data.get("requests"));
        }

        Responder responder = this.responder;
        if (responder != null) {
            processResponses((JsonArray) data.get("responses"));
        }
    }

    public void disconnected() {
        Responder responder = this.responder;
        if (responder != null) {
            responder.responderDisconnected();
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
