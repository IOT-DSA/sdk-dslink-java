package org.dsa.iot.dslink.methods;

import org.vertx.java.core.json.JsonArray;

/**
 * Generic response API
 *
 * @author Samuel Grenier
 */
public interface Response {

    /**
     * @return Request ID of the response
     */
    int getRid();

    /**
     * Populates the response with the incoming data.
     *
     * @param in Incoming data from remote endpoint
     */
    void populate(JsonArray in);
}
