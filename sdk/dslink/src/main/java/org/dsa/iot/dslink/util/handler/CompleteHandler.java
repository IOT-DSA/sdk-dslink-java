package org.dsa.iot.dslink.util.handler;

/**
 * Similar to a handler however it provides an easy way to determine
 * whether there are no more events.
 *
 * @author Samuel Grenier
 */
public interface CompleteHandler<T> extends Handler<T> {

    /**
     * All events are now completed.
     */
    void complete();
}
