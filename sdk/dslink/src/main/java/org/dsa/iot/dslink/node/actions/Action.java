package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Action API for handling invocations, parameters, and results.
 *
 * @author Samuel Grenier
 */
public class Action {

    private final JsonArray params = new JsonArray();
    private final JsonArray results = new JsonArray();

    private Permission permission;
    private final Handler<ActionResult> handler;
    private final InvokeMode mode;

    public Action(Permission permission,
                  Handler<ActionResult> handler) {
        this(permission, handler, InvokeMode.SYNC);
    }

    /**
     * @param permission Minimum required permission to invoke
     * @param handler    Handler for invocation
     * @param mode       Determines how the action should be invoked
     */
    public Action(Permission permission,
                  Handler<ActionResult> handler,
                  InvokeMode mode) {
        if (permission == null)
            throw new NullPointerException("permission");
        else if (handler == null)
            throw new NullPointerException("handler");
        else if (mode == null)
            throw new NullPointerException("mode");
        this.permission = permission;
        this.handler = handler;
        this.mode = mode;
    }

    /**
     * Updates the permission level of the action.
     *
     * @param permission New permission level
     */
    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    /**
     * @param parameter Add a parameter for the invocation
     * @return Current object for daisy chaining
     */
    public Action addParameter(Parameter parameter) {
        if (parameter == null) {
            throw new NullPointerException("parameter");
        }
        JsonObject param = paramToJson(parameter);
        if (param != null) {
            params.addObject(param);
        }
        return this;
    }

    /**
     * @param parameter Add a result for the invocation
     * @return Current object for daisy chaining;
     */
    public Action addResult(Parameter parameter) {
        if (parameter == null) {
            throw new NullPointerException("parameter");
        } else if (parameter.getDefault() != null) {
            String err = "parameter cannot contain a default value in a result";
            throw new IllegalStateException(err);
        }
        JsonObject result = paramToJson(parameter);
        if (result != null) {
            results.addObject(result);
        }
        return this;
    }

    /**
     * Determines whether to synchronously or asynchronously invoke
     * the action.
     *
     * @return How the action is invoked.
     */
    public InvokeMode getInvokeMode() {
        return mode;
    }

    /**
     * Invokes the action.
     *
     * @param result Result to populate as a result of invocation
     */
    public void invoke(ActionResult result) {
        if (!hasPermission()) return;
        handler.handle(result);
    }

    /**
     * @return Whether the user has permission to invoke
     */
    public boolean hasPermission() {
        return permission != Permission.NONE;
    }

    /**
     * @return Permission level of this action.
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * @return Parameters of the action.
     */
    public JsonArray getParams() {
        return params;
    }

    /**
     * @return The columns of the action
     */
    public JsonArray getColumns() {
        return results;
    }

    /**
     * Converts all the parameters to JSON consumable format.
     *
     * @param param Parameter to convert.
     * @return JSON object of the converted parameter.
     */
    private JsonObject paramToJson(Parameter param) {
        if (param == null)
            return null;
        JsonObject obj = new JsonObject();
        obj.putString("name", param.getName());
        obj.putString("type", param.getType().toJsonString());
        Value defVal = param.getDefault();
        if (defVal != null) {
            ValueUtils.toJson(obj, "default", defVal);
        }
        return obj;
    }

    /**
     * Determines how to invoke the action handler.
     */
    public enum InvokeMode {
        SYNC,
        ASYNC
    }
}
