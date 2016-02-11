package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.NodePair;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class RemoveResponse extends Response {

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
        obj.put("rid", rid);
        obj.put("stream", StreamState.CLOSED.getJsonName());
        return obj;
    }

    @Override
    public JsonObject getCloseResponse() {
        return null;
    }

    private void removeConfig() {
        String ref = pair.getReference();
        if (ref.startsWith("@")) {
            ref = ref.substring(1);
            pair.getNode().removeAttribute(ref);
        } else {
            throw new RuntimeException("Unable to set reference: " + ref);
        }
    }
}
