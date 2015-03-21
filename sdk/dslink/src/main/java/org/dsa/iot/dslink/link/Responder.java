package org.dsa.iot.dslink.link;

import org.dsa.iot.dslink.DSLinkHandler;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class Responder extends Linkable {

    public Responder(DSLinkHandler handler) {
        super(handler);
    }

    @Override
    public void parse(JsonObject in) {
        throw new UnsupportedOperationException();
    }

}
