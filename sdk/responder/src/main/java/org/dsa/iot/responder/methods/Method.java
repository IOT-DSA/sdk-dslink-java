package org.dsa.iot.responder.methods;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class Method {

    /**
     * @return An array of invocations
     */
    public abstract JsonObject invoke();

}
