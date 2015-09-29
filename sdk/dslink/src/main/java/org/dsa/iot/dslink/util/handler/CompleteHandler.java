package org.dsa.iot.dslink.util.handler;

/**
 * @author Samuel Grenier
 */
public interface CompleteHandler<T> extends Handler<T> {

    void complete(T event);
}
