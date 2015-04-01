package org.dsa.iot.dslink.methods.responses;

import org.dsa.iot.dslink.methods.Response;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class InvokeResponse implements Response {

    private final Node node;
    private final int rid;
    private JsonArray results;

    public InvokeResponse(int rid, Node node) {
        this.rid = rid;
        this.node = node;
    }

    @Override
    public int getRid() {
        return rid;
    }

    @Override
    public void populate(JsonObject in) {
        // TODO: Table API
        results = in.getArray("updates");
    }

    @Override
    public JsonObject getJsonResponse(JsonObject in) {
        Action action = node.getAction();
        if (action == null) {
            throw new RuntimeException("Node not invokable");
        }

        ActionResult results = new ActionResult(node, in);
        action.invoke(results);
        this.results = results.getUpdates();

        JsonObject obj = new JsonObject();
        obj.putNumber("rid", rid);
        obj.putString("stream", results.getStreamState().getJsonName());

        JsonArray cols = results.getColumns();
        if (cols == null) {
            cols = action.getColumns();
        }
        obj.putArray("columns", cols);
        obj.putArray("updates", this.results);
        return obj;
    }

    @Override
    public JsonObject getCloseResponse() {
        return null;
    }
}
