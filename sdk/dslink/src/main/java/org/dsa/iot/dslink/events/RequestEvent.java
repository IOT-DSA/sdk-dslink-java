package org.dsa.iot.dslink.events;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class RequestEvent {

    @NonNull private final Handler<Void> handler;
    private boolean called = false;
    
    public void call() {
        if (!called) {
            called = true;
            handler.handle(null);
        }
    }
}
