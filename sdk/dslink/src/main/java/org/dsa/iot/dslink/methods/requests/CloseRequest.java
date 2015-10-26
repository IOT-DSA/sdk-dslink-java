package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class CloseRequest extends Request {

    @Override
    public String getName() {
        return "close";
    }

    @Override
    public void addJsonValues(JsonObject out) {

    }
}
