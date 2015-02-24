package org.dsa.iot.dslink.requester.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.requester.requests.Request;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public abstract class Response<T extends Request> {

    @Getter
    @NonNull
    private final T request;

    /**
     * Handles the data returned from the response. The response
     * object should then be populated with its values.
     * @param array Parsed updates
     */
    public abstract void populate(JsonArray array);
}
