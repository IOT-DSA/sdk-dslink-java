package org.dsa.iot.responder.node.exceptions;

/**
 * @author Samuel Grenier
 */
public class NoSuchPath extends RuntimeException {

    public NoSuchPath(String msg) {
        super("Path does not exist: " + msg);
    }

}
