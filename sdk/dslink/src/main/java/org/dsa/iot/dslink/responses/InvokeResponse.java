package org.dsa.iot.dslink.responses;

import lombok.Getter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.requests.InvokeRequest;
import org.dsa.iot.dslink.util.ValueUtils;
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
