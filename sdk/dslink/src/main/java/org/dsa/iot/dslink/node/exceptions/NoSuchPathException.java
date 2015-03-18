package org.dsa.iot.dslink.node.exceptions;

/**
 * Thrown when no path can be located
 *
 * @author Samuel Grenier
 */
public class NoSuchPathException extends RuntimeException {

    /**
     * @param path Path that doesn't exist
     */
    public NoSuchPathException(String path) {
        super("No such path: " + path);
    }

}
