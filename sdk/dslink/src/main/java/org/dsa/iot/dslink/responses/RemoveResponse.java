package org.dsa.iot.dslink.responses;

import org.dsa.iot.dslink.requests.RemoveRequest;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public class RemoveResponse extends Response<RemoveRequest> {

    public RemoveResponse(RemoveRequest request) {
        super(request);
    }

    @Override
    public void populate(JsonArray o) {

    }
}
