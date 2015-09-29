package org.dsa.iot.dslink.util.handler;

/**
 * @author Samuel Grenier
 */
public interface Handler<T> {

    void handle(T event);

}
