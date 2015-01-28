package org.dsa.iot.dslink.responses;

import org.dsa.iot.dslink.requests.UnsubscribeRequest;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public class UnsubscribeResponse extends Response<UnsubscribeRequest> {

    public UnsubscribeResponse(UnsubscribeRequest request) {
        super(request);
    }

    @Override
    public void populate(JsonArray o) {

    }
}
