package org.dsa.iot.dslink.responses;

import org.dsa.iot.dslink.requests.SubscribeRequest;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public class SubscribeResponse extends Response<SubscribeRequest> {

    public SubscribeResponse(SubscribeRequest req) {
        super(req);
    }

    @Override
    public void populate(JsonArray o) {

    }
}
