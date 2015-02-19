package org.dsa.iot.dslink.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.dslink.connection.Client;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class RequestEvent {

    @Getter @NonNull private final Client client;
    @Getter @NonNull private final JsonObject request;
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
