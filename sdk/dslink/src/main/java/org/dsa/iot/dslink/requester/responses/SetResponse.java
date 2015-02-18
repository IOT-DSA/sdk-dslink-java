package org.dsa.iot.dslink.requester.responses;

import org.dsa.iot.dslink.requester.requests.SetRequest;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public class SetResponse extends Response<SetRequest> {

    public SetResponse(SetRequest request) {
        super(request);
    }

    @Override
    public void populate(JsonArray o) {

    }
}
