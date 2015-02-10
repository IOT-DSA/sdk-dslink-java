package org.dsa.iot.dslink.methods;

import com.google.common.eventbus.EventBus;
import lombok.AllArgsConstructor;
import org.dsa.iot.dslink.events.ClosedStreamEvent;
import org.dsa.iot.dslink.util.ResponseTracker;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class CloseMethod extends Method {

    private final EventBus bus;
    private final ResponseTracker tracker;
    private final int rid;

    @Override
    public JsonArray invoke(JsonObject request) {
        if (tracker.isTracking(rid)) {
            bus.post(new ClosedStreamEvent(rid));
            tracker.untrack(rid);
        }
        setState(StreamState.CLOSED);
        return null;
    }
}
