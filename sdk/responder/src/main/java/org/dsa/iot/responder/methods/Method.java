package org.dsa.iot.responder.methods;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public abstract class Method {

    private StreamState state;

    /**
     * @return An array of invocations
     */
    public abstract JsonObject invoke();

    public StreamState getState() {
        return state;
    }

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
