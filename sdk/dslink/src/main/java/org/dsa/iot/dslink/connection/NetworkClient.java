package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Handles writing and closing connections. Can be used for clients connected
 * to servers and remote endpoint connections.
 *
 * @author Samuel Grenier
 */
public interface NetworkClient {

    /**
     *
     * @return Whether the client can write immediately to the network without
     * being queued.
     */
    boolean writable();

    /**
     * Writes text data to the network.
     *
     * @param format Format the data should be encoded in.
     * @param data Data to write.
     */
    void write(EncodingFormat format, JsonObject data);

    /**
     * Closes the connection to the client
     */
    void close();

    /**
     * @return Whether the client is connected or not.
     */
    boolean isConnected();
}
