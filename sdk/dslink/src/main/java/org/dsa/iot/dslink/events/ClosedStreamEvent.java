package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.responder.Responder;
import org.dsa.iot.dslink.connection.Client;

/**
 * Posted when an open stream is closed.
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class ClosedStreamEvent extends Event {

    @NonNull private final Client client;
    @NonNull private final Responder responder;
    private final int rid;

}
