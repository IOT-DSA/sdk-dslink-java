package org.dsa.iot.dslink.responses;

import org.dsa.iot.dslink.requests.SetRequest;
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
