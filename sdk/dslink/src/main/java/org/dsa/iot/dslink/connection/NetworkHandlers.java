package org.dsa.iot.dslink.connection;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

/**
 * Handles network clients on vertx events.
 *
 * @author Samuel Grenier
 */
class NetworkHandlers {

    private Handler<Void> onConnected;
    private Handler<Void> onDisconnected;
    private Handler<Throwable> onException;
    private Handler<Buffer> onData;

    public Handler<Void> getOnConnected() {
        return onConnected;
    }

    public void setOnConnected(Handler<Void> onConnected) {
        this.onConnected = onConnected;
    }

    public Handler<Void> getOnDisconnected() {
        return onDisconnected;
    }

    public void setOnDisconnected(Handler<Void> onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public Handler<Throwable> getOnException() {
        return onException;
    }

    public void setOnException(Handler<Throwable> onException) {
        this.onException = onException;
    }

    public Handler<Buffer> getOnData() {
        return onData;
    }

    public void setOnData(Handler<Buffer> onData) {
        this.onData = onData;
    }
}
