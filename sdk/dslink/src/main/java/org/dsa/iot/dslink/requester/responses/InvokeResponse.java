package org.dsa.iot.dslink.requester.responses;

import lombok.Getter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requester.requests.InvokeRequest;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
public class InvokeResponse extends Response<InvokeRequest> {

    private Value value;

    public InvokeResponse(InvokeRequest request) {
        super(request);
    }

    @Override
    public void populate(JsonArray o) {
        JsonObject obj = o.get(0);
        value = ValueUtils.toValue(obj.getField("result"));
    }
}
