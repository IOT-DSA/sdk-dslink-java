package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class ContinuousInvokeRequest extends Request {

    private final JsonObject params;

    public ContinuousInvokeRequest(JsonObject params) {
        this.params = params;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void addJsonValues(JsonObject out) {
        if (params != null) {
            out.put("params", params);
        }
    }
}
