package org.dsa.iot.dslink.responder.methods;

import lombok.AllArgsConstructor;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.responder.Responder;
import org.dsa.iot.dslink.events.ClosedStreamEvent;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.responder.ResponseTracker;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class CloseMethod extends Method {

    private final MBassador<Event> bus;
    private final Client client;
    private final ResponseTracker tracker;
    private final Responder responder;
    private final int rid;

    @Override
    public JsonArray invoke(JsonObject request) {
        if (tracker.isTracking(rid)) {
            bus.publish(new ClosedStreamEvent(client, responder, rid));
            tracker.untrack(rid);
        }
        setState(StreamState.CLOSED);
        return null;
    }
}
