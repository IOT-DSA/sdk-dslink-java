package org.dsa.iot.dslink.connection;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
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
     * Sets the handler used for managing incoming requests from the endpoint.
     *
     * @param handler Handler to set with an array of requests.
     */
    void setRequestDataHandler(Handler<JsonArray> handler);

    /**
     * Sets the handler used for managing incoming responses from the endpoint.
     *
     * @param handler Handler to set with an array of responses
     */
    void setResponseDataHandler(Handler<JsonArray> handler);

    /**
     * @return Whether this client is a requester or not.
     */
    boolean isRequester();

    /**
     * @return Whether this client is a responder or not.
     */
    boolean isResponder();
}
