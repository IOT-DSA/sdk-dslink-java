package org.dsa.iot.dslink.node.exceptions;

/**
 * @author Samuel Grenier
 */
public class NoSuchPathException extends RuntimeException {

    public NoSuchPathException(String msg) {
        super("Path does not exist: " + msg);
    }

}
