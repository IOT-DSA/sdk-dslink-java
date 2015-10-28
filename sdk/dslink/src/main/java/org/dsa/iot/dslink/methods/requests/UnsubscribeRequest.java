package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.List;

/**
 * Unsubscribes to the designated subscription IDs.
 *
 * @author Samuel Grenier
 */
public class UnsubscribeRequest extends Request {

    private final List<Integer> sids;

    public UnsubscribeRequest(List<Integer> sids) {
        if (sids == null) {
            throw new IllegalArgumentException("sids");
        }
        this.sids = sids;
    }

    @Override
    public String getName() {
        return "unsubscribe";
    }

    @Override
    public void addJsonValues(JsonObject out) {
        out.put("sids", new JsonArray(sids));
    }
}
