package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.Permission;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Action API for handling invocations, parameters, and results.
 *
 * @author Samuel Grenier
 */
public class Action {

    private final List<Parameter> params = new ArrayList<>();
    private final List<Parameter> results = new ArrayList<>();

    private final String name;
    private final Permission permission;
    private final Handler<ActionResult> handler;

    /**
     * @param name       Name of the action
     * @param permission Minimum required permission to invoke
     * @param handler    Handler for invocation
     */
    public Action(String name, Permission permission, Handler<ActionResult> handler) {
        if (name == null)
            throw new NullPointerException("name");
        else if (permission == null)
            throw new NullPointerException("permission");
        else if (handler == null)
            throw new NullPointerException("handler");
        this.name = name;
        this.permission = permission;
        this.handler = handler;
    }

    /**
     * @return Serializable name of the action.
     */
    public String getName() {
        return name;
    }

    /**
     * @param parameter Add a parameter for the invocation
     */
    public void addParameter(Parameter parameter) {
        params.add(parameter);
    }

    /**
     * @param parameter Add a result for the invocation
     */
    public void addResult(Parameter parameter) {
        results.add(parameter);
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
     * Converts the action to JSON and adds it to the designated JSON object.
     *
     * @param obj Object to alternate.
     */
    public void toJson(JsonObject obj) {
        if (!hasPermission()) return;
        obj.putString("$invokable", permission.getJsonName());
        obj.putArray("$params", paramsToJson(params));
        obj.putArray("$columns", paramsToJson(results));
    }

    /**
     * @return The columns of the action
     */
    public JsonArray getColumns() {
        return paramsToJson(results);
    }

    /**
     * Converts all the parameters to JSON consumable format.
     *
     * @param p List of parameters
     * @return JSON array of parameters converted to JSON values.
     */
    private JsonArray paramsToJson(List<Parameter> p) {
        JsonArray array = new JsonArray();
        if (p != null) {
            for (Parameter param : p) {
                JsonObject obj = new JsonObject();
                obj.putString("name", param.getName());
                obj.putString("type", param.getType().toJsonString());
                array.add(obj);
            }
        }
        return array;
    }
}
