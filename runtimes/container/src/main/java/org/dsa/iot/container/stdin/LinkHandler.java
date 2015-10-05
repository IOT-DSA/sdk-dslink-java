package org.dsa.iot.container.stdin;

/**
 * @author Samuel Grenier
 */
public interface LinkHandler {

    public static final String THREAD_NAME = "input-handler";

    void start();
}
