package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class MessageProcessor {

    private final DSLinkNode node;

    private final Responder responder;
    private final Requester requester;

    public MessageProcessor(DSLinkNode node) {
        this.node = node;
        this.responder = new Responder(node);
        this.requester = new Requester(node);
    }

    public void processData(JsonObject data) {
        if (client().handshake().isRequester()) {
            processRequests((JsonArray) data.get("requests"));
        }
        if (client().handshake().isResponder()) {
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

    protected Client client() {
        return node.client();
    }
}
