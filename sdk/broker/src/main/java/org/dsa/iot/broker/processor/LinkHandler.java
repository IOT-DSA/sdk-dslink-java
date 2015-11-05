package org.dsa.iot.broker.processor;

import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.server.client.Client;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class LinkHandler {

    private final DSLinkNode node;

    public LinkHandler(DSLinkNode node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        this.node = node;
    }

    public DSLinkNode node() {
        return node;
    }

    public Client client() {
        return node.client();
    }

    protected abstract void process(JsonObject obj);
}
