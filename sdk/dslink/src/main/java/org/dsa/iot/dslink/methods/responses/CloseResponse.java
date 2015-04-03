package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class CloseResponse implements Response {

    private final int rid;
    private final Response response;

    public CloseResponse(int rid, Response response) {
        this.rid = rid;
        this.response = response;
    }

    @Override
    public int getRid() {
        return rid;
    }

    @Override
    public void populate(JsonObject in) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject getJsonResponse(JsonObject in) {
        if (response == null) {
            JsonObject obj = new JsonObject();
            obj.putNumber("rid", rid);
            obj.putString("stream", StreamState.CLOSED.getJsonName());
            return obj;
        } else {
            return response.getCloseResponse();
        }
    }

    @Override
    public JsonObject getCloseResponse() {
        throw new UnsupportedOperationException();
    }
}
