package org.dsa.iot.dslink.responder.methods;

import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.events.ClosedStreamEvent;
import org.dsa.iot.dslink.responder.Responder;
import org.dsa.iot.dslink.responder.ResponseTracker;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class CloseMethod extends Method {

    private final MBassador<Event> bus;
    private final Client client;
    private final ResponseTracker tracker;
    private final Responder responder;
    private final int rid;

    public CloseMethod(MBassador<Event> bus,
                        Client client,
                        ResponseTracker tracker,
                        Responder responder,
                        int rid,
                        JsonObject request) {
        super(request);
        this.bus = bus;
        this.client = client;
        this.tracker = tracker;
        this.responder = responder;
        this.rid = rid;
    }

    @Override
    public JsonArray invoke() {
        if (tracker.isTracking(rid)) {
            bus.publish(new ClosedStreamEvent(client, responder, rid));
            tracker.untrack(rid);
        }
        setState(StreamState.CLOSED);
        return null;
    }
}
