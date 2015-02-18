package org.dsa.iot.dslink.requester.responses;

import org.dsa.iot.dslink.requester.requests.CloseRequest;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public class CloseResponse extends Response<CloseRequest> {

    public CloseResponse(CloseRequest request) {
        super(request);
    }

    @Override
    public void populate(JsonArray o) {

    }
}
