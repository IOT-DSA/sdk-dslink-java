package org.dsa.iot.dslink.responder.methods;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class Method {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private StreamState state;

    /**
     * @param request Original request body
     * @return An array of update responses
     */
    public abstract JsonArray invoke(JsonObject request);
}
