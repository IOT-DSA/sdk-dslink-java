package org.dsa.iot.dslink.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.connection.Client;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * Posted on incoming requests from clients.
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public class RequestEvent extends Event {

    @NonNull private final Client client;
    @NonNull private final JsonObject request;
    
    private final int rid;
    private final String method;
}
