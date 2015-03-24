package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class CloseRequest implements Request {

    @Override
    public String getName() {
        return "close";
    }

    @Override
    public void addJsonValues(JsonObject out) {

    }
}
