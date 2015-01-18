package org.dsa.iot.responder.node.exceptions;

/**
 * @author Samuel Grenier
 */
public class NoSuchPathException extends RuntimeException {

    public NoSuchPathException() {
    }

    public NoSuchPathException(String msg) {
        super("Path does not exist: " + msg);
    }

}
