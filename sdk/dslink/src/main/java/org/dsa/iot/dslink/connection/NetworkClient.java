package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.requester.Requester;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * Handles writing and closing connections. Can be used for clients connected
 * to servers and remote endpoint connections.
 *
 * @author Samuel Grenier
 */
public interface NetworkClient {

    /**
     * Writes the object to the remote endpoint.
     *
     * @param object Object to write
     */
    void write(JsonObject object);

    /**
     * Closes the connection to the client
     */
    void close();

    /**
     * Sets the handler used for managing incoming data from the endpoint.
     *
     * @param handler Handler to set
     */
    void setDataHandler(Handler<JsonObject> handler);

    /**
     * @return The requester the client is using.
     */
    Requester getRequester();

    /**
     * Sets the requester of the client for which it can perform requests
     * on the client.
     * @param requester Requester that the client will use.
     */
    void setRequester(Requester requester);
}
