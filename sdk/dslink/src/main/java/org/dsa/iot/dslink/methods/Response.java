package org.dsa.iot.dslink.methods;

import org.vertx.java.core.json.JsonObject;

/**
 * Generic response API
 *
 * @author Samuel Grenier
 */
public interface Response {

    /**
     * @return Request ID of the response
     */
    int getRid();

    /**
     * Populates the response with the incoming data.
     *
     * @param in Incoming data from remote endpoint
     */
    void populate(JsonObject in);

    /**
     * Retrieves a response object based on the incoming request.
     *
     * @param in Original incoming data
     * @return JSON response
     */
    JsonObject getJsonResponse(JsonObject in);

    /**
     * Closes the stream if it is open and writes the closed stream response
     * to the client. This response should then go into a closed state.
     *
     * @return Closed state response information
     * @see org.dsa.iot.dslink.methods.responses.CloseResponse
     */
    JsonObject getCloseResponse();
}
