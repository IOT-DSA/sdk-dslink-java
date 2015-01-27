package org.dsa.iot.dslink.requests;

import lombok.AllArgsConstructor;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public abstract class Request {

    /**
     * @return Name of the method to invoke
     */
    public abstract String getName();

    /**
     * @param obj Add values to this object for writing to a JSON request. Do
     *            NOT overwrite "rid" and "method" unless you know what you are
     *            doing.
     */
    public abstract void addJsonValues(JsonObject obj);
}
