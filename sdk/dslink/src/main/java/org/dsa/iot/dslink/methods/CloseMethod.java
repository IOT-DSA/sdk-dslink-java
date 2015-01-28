package org.dsa.iot.dslink.methods;

import lombok.AllArgsConstructor;
import org.dsa.iot.dslink.util.ResponseTracker;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class CloseMethod extends Method {

    private final ResponseTracker tracker;
    private final int rid;

    @Override
    public JsonArray invoke(JsonObject request) {
        tracker.untrack(rid);
        setState(StreamState.CLOSED);
        return null;
    }
}
