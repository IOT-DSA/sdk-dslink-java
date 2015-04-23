package org.dsa.iot.dslink.connection;

/**
 * Handles writing and closing connections. Can be used for clients connected
 * to servers and remote endpoint connections.
 *
 * @author Samuel Grenier
 */
public interface NetworkClient {

    /**
     * Writes data to the network.
     *
     * @param data Data to write
     */
    void write(String data);

    /**
     * Closes the connection to the client
     */
    void close();

    /**
     * @return Whether the client is connected or not.
     */
    boolean isConnected();
}
