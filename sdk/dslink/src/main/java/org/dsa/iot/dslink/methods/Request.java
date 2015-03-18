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
     * Add the json values of the specified request to the JSON so it can be
     * sent to a remote endpoint.
     *
     * @param out Values to add to
     */
    void addJsonValues(JsonObject out);
}
