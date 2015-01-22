package org.dsa.iot.responder.node.exceptions;

/**
 * @author Samuel Grenier
 */
public class DuplicateException extends RuntimeException {

    public DuplicateException(String msg) {
        super(msg);
    }
}
