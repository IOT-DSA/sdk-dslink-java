package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
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
     * If the action result has dynamic columns this can be set. If the columns
     * remains {@code null} then the default columns from the defined action
     * will be used.
     */
    private JsonArray columns;

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
     * @see #getParameter
     */
    @Deprecated
    public JsonObject getJsonIn() {
        return jsonIn;
    }

    /**
     * Gets a parameter from the incoming parameters from the endpoint. The
     * returned parameter is unchecked.
     *
     * @param name Name of the parameter.
     * @return Returns a value of {@code null} if it doesn't exist.
     */
    public Value getParameter(String name) {
        return getParameter(name, (Value) null);
    }

    /**
     * Gets a parameter from the incoming parameters from the endpoint. The
     * parameter is checked to make sure it exists and matches the type.
     *
     * @param name Name of the parameter.
     * @param type Value type the parameter must have.
     * @return Returns a value.
     */
    public Value getParameter(String name, ValueType type) {
        Value ret = getParameter(name);
        if (ret == null) {
            throw new RuntimeException("Missing parameter: " + name);
        }
        checkType(name, type, ret);
        return ret;
    }

    /**
     * Gets a parameter from the incoming parameters from the endpoint. The
     * default value type is checked against the type of the parameter as
     * received from the endpoint. If the default value is {@code null} then
     * then type is unchecked and a value of {@code null} can be returned.
     *
     * @param name Name of the parameter.
     * @param def Default value to return if the parameter doesn't exist.
     * @return Returns a value.
     */
    public Value getParameter(String name, Value def) {
        JsonObject params = jsonIn.getObject("params");
        if (params == null) {
            return def;
        }

        Object obj = params.getField(name);
        if (obj == null) {
            return def;
        }

        Value ret = ValueUtils.toValue(obj);
        if (def != null) {
            checkType(name, def.getType(), ret);
        }
        return ret;
    }

    /**
     * Gets the columns of the result. If no columns were set then the
     * default columns sent will be from the defined action.
     *
     * @return Columns of the result
     */
    public JsonArray getColumns() {
        return columns;
    }

    /**
     * When the columns are sent, these columns override the default columns
     * of the action. This allows for dynamic columns to be sent.
     *
     * @param columns Dynamic columns to set.
     */
    public void setColumns(JsonArray columns) {
        this.columns = columns;
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

    private void checkType(String name, ValueType type, Value value) {
        if (value == null || type != value.getType()) {
            String t = value == null ? "null" : value.getType().toJsonString();
            throw new RuntimeException("Parameter " + name + " has a bad type of " + t);
        }
    }
}
