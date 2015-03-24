package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

/**
 * Unsubscribes to the designated subscription IDs.
 *
 * @author Samuel Grenier
 */
public class UnsubscribeRequest implements Request {

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
        out.putArray("sids", new JsonArray(sids));
    }
}
