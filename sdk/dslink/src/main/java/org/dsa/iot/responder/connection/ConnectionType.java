package org.dsa.iot.responder.connection;

/**
 * @author Samuel Grenier
 */
public enum ConnectionType {

    /**
     * Connects to the server using a raw socket connection.
     */
    SOCKET,

    /**
     * Connects to the server constantly using http in a polling loop.
     */
    HTTP,

    /**
     * Connects to the server using websockets, similar to sockets
     */
    WS
}
