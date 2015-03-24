package org.dsa.iot.dslink.methods;

import org.vertx.java.core.json.JsonObject;

/**
 * Generic request API
 *
 * @author Samuel Grenier
 */
public interface Request {

    /**
     * @return The name of the request
     */
    String getName();

    /**
     * Add the JSON request values of the specified request to the JSON to
     * send to the responder.
     *
     * @param out Values to add to.
     */
    void addJsonValues(JsonObject out);
}
