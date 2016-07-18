package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * The results from an invoked action that are set here.
 *
 * @author Samuel Grenier
 */
public class ActionResult {

    /**
     * Node this action is modifying.
     */
    private final Node node;

    /**
     * Data object to act upon for the invocation.
     */
    private final JsonObject jsonIn;

    /**
     * Table of results returned from the action.
     */
    private Table table;

    /**
     * Stream state can be set to prevent a closing stream after invocation.
     * The default state is to immediately close the invocation response.
     */
    private StreamState state = StreamState.CLOSED;

    /**
     * Callback for when close is called on this action.
     */
    private Handler<Void> closeHandler;

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
    @SuppressWarnings("unused")
    public Value getParameter(String name, ValueType type) {
        Value ret = getParameter(name);
        if (ret == null) {
            throw new RuntimeException("Missing parameter: " + name);
        }
        ValueUtils.checkType(name, type, ret);
        return ret;
    }

    /**
     * Gets the input JSON.
     * @return JSON input.
     */
    public JsonObject getJsonIn() {
        return jsonIn;
    }

    /**
     * Get the input parameters.
     * @return input parameters
     */
    public JsonObject getParameters() {
        JsonObject input = getJsonIn();
        if (input.contains("params")) {
            return input.get("params");
        } else {
            return new JsonObject();
        }
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
        JsonObject params = jsonIn.get("params");
        if (params == null) {
            return def;
        }

        Object obj = params.get(name);
        if (obj == null) {
            return def;
        }

        Value ret = ValueUtils.toValue(obj);
        if (def != null) {
            ValueUtils.checkType(name, def.getType(), ret);
        }
        return ret;
    }

    /**
     * The table to add rows to.
     *
     * @return Table of the result.
     */
    public Table getTable() {
        if (table == null) {
            return table = new Table();
        }
        return table;
    }

    /**
     * Sets the table of results to be returned. This method is not necessary
     * if a custom {@link Table} instance is not being used.
     * {@link #getTable()} is sufficient for a standard table.
     *
     * @param table Table to set.
     */
    public void setTable(Table table) {
        if (table == null) {
            throw new NullPointerException("table");
        }
        this.table = table;
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

    /**
     * Sets the close handler to be called when the invocation is being closed.
     *
     * @param handler Close handler callback.
     */
    public void setCloseHandler(Handler<Void> handler) {
        this.closeHandler = handler;
    }

    /**
     * @return Close handler callback.
     */
    public Handler<Void> getCloseHandler() {
        return closeHandler;
    }
}
