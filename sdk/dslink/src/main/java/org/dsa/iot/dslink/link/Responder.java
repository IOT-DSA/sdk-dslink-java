package org.dsa.iot.dslink.link;

import org.dsa.iot.dslink.DSLinkHandler;
import org.vertx.java.core.json.JsonObject;

/**
 * Handles incoming requests and outgoing responses.
 *
 * @author Samuel Grenier
 */
public class Responder extends Linkable {

    public Responder(DSLinkHandler handler) {
        super(handler);
    }

    /**
     * Handles incoming requests
     *
     * @param in Incoming request
     * @return Outgoing response
     */
    public JsonObject parse(JsonObject in) {
        throw new UnsupportedOperationException();
    }

}
