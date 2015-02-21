package org.dsa.iot.dslink.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.connection.Client;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class RequestEvent extends Event {

    @Getter @NonNull private final Client client;
    @Getter @NonNull private final JsonObject request;
    
    @Getter private final int rid;
    @Getter private final String method;
    
    @NonNull private final Handler<Void> handler;
    private boolean locked = false;
    private boolean called = false;
    
    public synchronized void call() {
        if (!(called || locked)) {
            called = true;
            handler.handle(null);
        }
    }
    
    public synchronized void setLocked(boolean locked) {
        this.locked = locked;
    }
}
