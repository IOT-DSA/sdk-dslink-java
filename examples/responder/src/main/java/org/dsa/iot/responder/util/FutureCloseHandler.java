package org.dsa.iot.responder.util;


import org.dsa.iot.dslink.util.handler.Handler;

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
