package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class UnsubscribeResponse implements Response {

    private final int rid;
    private final SubscriptionManager manager;

    public UnsubscribeResponse(int rid, DSLink link) {
        this.rid = rid;
        this.manager = link.getSubscriptionManager();
    }

    @Override
    public int getRid() {
        return rid;
    }

    @Override
    public void populate(JsonObject in) {
    }

    @Override
    public JsonObject getJsonResponse(JsonObject in) {
        JsonArray sids = in.getArray("sids");
        if (sids != null && sids.size() > 0) {
            for (Object obj : sids) {
                Integer sid = (Integer) obj;
                manager.removeValueSub(sid);
            }
        }

        JsonObject obj = new JsonObject();
        obj.putNumber("rid", rid);
        obj.putString("stream", StreamState.CLOSED.getJsonName());
        return obj;
    }

    @Override
    public JsonObject getCloseResponse() {
        return null;
    }
}
