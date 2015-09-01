package org.dsa.iot.responder.util;


import org.vertx.java.core.Handler;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Samuel Grenier
 */
public class FutureCloseHandler implements Handler<Void> {

    private ScheduledFuture<?> fut;

    public FutureCloseHandler(ScheduledFuture<?> fut) {
        this.fut = fut;
    }

    @Override
    public void handle(Void event) {
        if (fut != null) {
            fut.cancel(false);
        }
    }
}
