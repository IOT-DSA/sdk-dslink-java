package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.NodePair;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.json.JsonObject;

/**
 * Handles a set response
 *
 * @author Samuel Grenier
 */
public class SetResponse implements Response {

    private final int rid;
    private final NodePair pair;

    public SetResponse(int rid, NodePair pair) {
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
        updateNode(in);

        JsonObject obj = new JsonObject();
        obj.putNumber("rid", rid);
        obj.putString("stream", StreamState.CLOSED.getJsonName());
        return obj;
    }

    @Override
    public JsonObject getCloseResponse() {
        return null;
    }

    private void updateNode(JsonObject in) {
        String ref = pair.getReference();
        Value value = ValueUtils.toValue(in.getObject("value"));
        if (ref.startsWith("$")) {
            ref = ref.substring(1);
            pair.getNode().setConfig(ref, value);
        } else if (ref.startsWith("@")) {
            ref = ref.substring(1);
            pair.getNode().setAttribute(ref, value);
        } else {
            throw new RuntimeException("Not a reference: " + ref);
        }
    }
}
