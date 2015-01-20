package org.dsa.iot.responder.methods;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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
     * @return An array of invocations
     */
    public abstract JsonObject invoke(JsonObject request);

    public enum StreamState {
        INITIALIZED("initialize"),
        OPEN("open"),
        CLOSED("closed");

        public final String jsonName;

        private StreamState(String jsonName) {
            this.jsonName = jsonName;
        }
    }
}
