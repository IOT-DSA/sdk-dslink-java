package org.dsa.iot.responder.methods;

import lombok.AllArgsConstructor;
import org.dsa.iot.responder.node.RequestTracker;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class CloseMethod extends Method {

    private final RequestTracker tracker;
    private final int rid;

    @Override
    public JsonObject invoke(JsonObject request) {
        tracker.untrack(rid);
        setState(StreamState.CLOSED);
        return null;
    }
}
