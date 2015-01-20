package org.dsa.iot.core;

import org.vertx.java.core.Handler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Turns an asynchronous handler into a synchronous handler.
 * @author Samuel Grenier
 */
public class SyncHandler<T> implements Handler<T> {

    private final BlockingQueue<T> queue = new ArrayBlockingQueue<>(1);

    public T get() {
        return get(-1, null);
    }

    public T get(int timeout, TimeUnit unit) {
        try {
            if (timeout == -1)
                return queue.take();
            else
                return queue.poll(timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final void handle(T event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
