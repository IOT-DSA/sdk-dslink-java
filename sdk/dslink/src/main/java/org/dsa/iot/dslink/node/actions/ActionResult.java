package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * The results from an invoked action that are set here.
 *
 * @author Samuel Grenier
 */
public class ActionResult {

    /**
     * Node this action is modifying
     */
    private final Node node;

    /**
     * Data object to act upon for the invocation.
     */
    private final JsonObject jsonIn;

    /**
     * Updates returned from the list response
     */
    private JsonArray updates;

    /**
     * Stream state can be set to prevent a closing stream after invocation.
     * The default state is to immediately close the invocation response.
     */
    private StreamState state = StreamState.CLOSED;

    /**
     * Creates an action result that is ready to be invoked and populated
     * with results.
     *
     * @param node The node this action is invoked on.
     * @param in Incoming JSON data.
     */
    public ActionResult(Node node, JsonObject in) {
        if (node == null)
            throw new NullPointerException("node");
        else if (in == null)
            throw new NullPointerException("in");
        this.node = node;
        this.jsonIn = in;
    }

    /**
     * @return Node the action is acting on.
     */
    public Node getNode() {
        return node;
    }

    /**
     * @return Original invocation request.
     */
    public JsonObject getJsonIn() {
        return jsonIn;
    }

    /**
     * @return JSON updates as a result of the invocation
     */
    public JsonArray getUpdates() {
        return updates;
    }

    /**
     * @param updates The invocation results.
     */
    public void setUpdates(JsonArray updates) {
        this.updates = updates;
    }

    /**
     * Set the stream state to open as needed
     *
     * @return Stream state as a result of the invocation.
     */
    public StreamState getStreamState() {
        return state;
    }

    /**
     * By default, the state is set to closed unless set.
     *
     * @param state Sets the stream state of the action invocation.
     */
    public void setStreamState(StreamState state) {
        if (state == null)
            throw new NullPointerException("state");
        this.state = state;
    }
}
