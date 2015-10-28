package org.dsa.iot.dslink.methods;

import org.dsa.iot.dslink.methods.responses.ErrorResponse;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * Generic response API
 *
 * @author Samuel Grenier
 */
public abstract class Response {

    private ErrorResponse error;

    /**
     * Sets the error that occurred in the originating request.
     *
     * @param error Error to set.
     */
    public void setError(ErrorResponse error) {
        this.error = error;
    }

    /**
     * @return The originating error that was set or {@code null} if there was
     * no error.
     */
    public ErrorResponse getError() {
        return error;
    }

    /**
     * @return Whether or not an error has occurred.
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * @return Request ID of the response
     */
    public abstract int getRid();

    /**
     * Populates the response with the incoming data.
     *
     * @param in Incoming data from remote endpoint
     */
    public abstract void populate(JsonObject in);

    /**
     * Retrieves a response object based on the incoming request.
     *
     * @param in Original incoming data
     * @return JSON response
     */
    public abstract JsonObject getJsonResponse(JsonObject in);

    /**
     * Closes the stream if it is open and writes the closed stream response
     * to the client. This response should then go into a closed state.
     *
     * @return Closed state response information
     * @see org.dsa.iot.dslink.methods.responses.CloseResponse
     */
    public abstract JsonObject getCloseResponse();
}
