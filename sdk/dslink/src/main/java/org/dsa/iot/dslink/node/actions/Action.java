package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.Permission;
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

    private final Permission permission;
    private final Handler<ActionResult> handler;

    /**
     * @param permission Minimum required permission to invoke
     * @param handler    Handler for invocation
     */
    public Action(Permission permission, Handler<ActionResult> handler) {
        if (permission == null)
            throw new NullPointerException("permission");
        else if (handler == null)
            throw new NullPointerException("handler");
        this.permission = permission;
        this.handler = handler;
    }

    /**
     * @param parameter Add a parameter for the invocation
     */
    public void addParameter(Parameter parameter) {
        JsonObject param = paramToJson(parameter);
        if (param != null) {
            params.addObject(param);
        }
    }

    /**
     * @param parameter Add a result for the invocation
     */
    public void addResult(Parameter parameter) {
        JsonObject result = paramToJson(parameter);
        if (result != null) {
            results.addObject(result);
        }
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
        return obj;
    }
}
