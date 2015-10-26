package org.dsa.iot.dslink.methods;

import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Generic request API
 *
 * @author Samuel Grenier
 */
public abstract class Request {

    /**
     * @return The name of the request
     */
    public abstract String getName();

    /**
     * Add the JSON request values of the specified request to the JSON to
     * send to the responder.
     *
     * @param out Values to add to.
     */
    public abstract void addJsonValues(JsonObject out);
}
