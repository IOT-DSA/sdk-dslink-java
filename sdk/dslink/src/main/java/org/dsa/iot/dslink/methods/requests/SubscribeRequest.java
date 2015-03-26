package org.dsa.iot.dslink.methods.requests;

import org.dsa.iot.dslink.methods.Request;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class SubscribeRequest implements Request {

    private final Map<String, Integer> subs;

    /**
     * @param subs Map of path to sid subscriptions
     */
    public SubscribeRequest(Map<String, Integer> subs) {
        if (subs == null) {
            throw new NullPointerException("subs");
        }
        this.subs = subs;
    }

    @Override
    public String getName() {
        return "subscribe";
    }

    @Override
    public void addJsonValues(JsonObject out) {
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Integer> sub : subs.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.putString("path", sub.getKey());
            obj.putNumber("sid", sub.getValue());
            array.addObject(obj);
        }
        out.putArray("paths", array);
    }
}
