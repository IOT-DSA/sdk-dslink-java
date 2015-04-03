package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.NodePair;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class RemoveResponse implements Response {

    private final int rid;
    private final NodePair pair;

    public RemoveResponse(int rid, NodePair pair) {
        if (pair.getReference() == null) {
            String err = "path does not reference config or attribute";
            throw new NullPointerException(err);
        }
        this.rid = rid;
        this.pair = pair;
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
        removeConfig();

        JsonObject obj = new JsonObject();
        obj.putNumber("rid", rid);
        obj.putString("stream", StreamState.CLOSED.getJsonName());
        return obj;
    }

    @Override
    public JsonObject getCloseResponse() {
        return null;
    }

    private void removeConfig() {
        String ref = pair.getReference();
        if (ref.startsWith("$")) {
            ref = ref.substring(1);
            pair.getNode().removeConfig(ref);
        } else if (ref.startsWith("@")) {
            ref = ref.substring(1);
            pair.getNode().removeAttribute(ref);
        } else {
            throw new RuntimeException("Not a reference: " + ref);
        }
    }
}
