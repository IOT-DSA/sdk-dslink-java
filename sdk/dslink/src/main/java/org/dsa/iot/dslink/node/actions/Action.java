package org.dsa.iot.dslink.node.actions;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Collection;

/**
 * Action API for handling invocations, parameters, and results.
 *
 * @author Samuel Grenier
 */
public class Action {

    protected JsonArray params = new JsonArray();
    protected JsonArray results = new JsonArray();

    private SubscriptionManager manager;
    private Node node;

    private Permission permission;
    private ResultType resultType;
    private boolean hidden;

    private final Handler<ActionResult> handler;

    /**
     * The default result type is {@link ResultType#VALUES}. This value must
     * be changed accordingly.
     *
     * @param permission Minimum required permission to invoke.
     * @param handler    Handler for invocation. Allowed to be {@code null}
     *                   for profiles.
     */
    public Action(Permission permission,
                  Handler<ActionResult> handler) {
        if (permission == null)
            throw new NullPointerException("permission");
        this.resultType = ResultType.VALUES;
        this.permission = permission;
        this.handler = handler;
    }

    /**
     * Used to the action as hidden from DSA. This allows the creation of
     * action profiles
     *
     * @param hidden Whether to hide the action or not.
     */
    @SuppressWarnings("unused")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Updates the permission level of the action.
     *
     * @param permission New permission level
     */
    public void setPermission(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        this.permission = permission;
    }

    /**
     * The default result type {@link ResultType#VALUES} and must be
     * set accordingly.
     *
     * @param type The new result type to set.
     * @return Current object for daisy chaining.
     */
    public Action setResultType(ResultType type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.resultType = type;
        return this;
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
            params.add(param);
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
            String err = "Parameter cannot contain a default value in a result";
            throw new IllegalStateException(err);
        }
        JsonObject result = paramToJson(parameter);
        if (result != null) {
            results.add(result);
        }
        return this;
    }

    /**
     * Clears the existing parameters and adds the new parameters.
     *
     * @param newParams Parameters to set.
     */
    public void setParams(Collection<Parameter> newParams) {
        this.params = new JsonArray();
        for (Parameter p : newParams) {
            addParameter(p);
        }
        postParamsUpdate();
    }

    /**
     * Clears the existing columns and adds the new columns.
     *
     * @param newColumns Columns to set.
     */
    public void setColumns(Collection<Parameter> newColumns) {
        this.results = new JsonArray();
        for (Parameter p : newColumns) {
            addResult(p);
        }
        postColumnsUpdate();
    }

    public void setSubscriptionManager(Node node,
                                       SubscriptionManager manager) {
        this.node = node;
        this.manager = manager;
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
     * Hidden actions are used for action profiles.
     *
     * @return Whether the action is hidden or not.
     */
    public boolean isHidden() {
        return hidden;
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
     * @return The result type this invocation returns.
     */
    public ResultType getResultType() {
        return resultType;
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
     * Posts a parameter update.
     */
    protected final void postParamsUpdate() {
        if (node != null && manager != null) {
            Value v = new Value(getParams());
            manager.postMetaUpdate(node, "$params", v);
        }
    }

    /**
     * Posts a columns update.
     */
    protected final void postColumnsUpdate() {
        if (node != null && manager != null) {
            Value v = new Value(getColumns());
            manager.postMetaUpdate(node, "$columns", v);
        }
    }

    /**
     * Converts all the parameters to JSON consumable format.
     *
     * @param param Parameter to convert.
     * @return JSON object of the converted parameter.
     */
    protected JsonObject paramToJson(Parameter param) {
        if (param == null)
            return null;
        JsonObject obj = new JsonObject();
        obj.put("name", param.getName());
        obj.put("type", param.getType().toJsonString());
        Value defVal = param.getDefault();
        if (defVal != null) {
            obj.put("default", defVal);
        }

        EditorType type = param.getEditorType();
        if (type != null) {
            obj.put("editor", type.toJsonString());
            if (type.getMeta() != null) {
                obj.put("editorMeta", type.getMeta());
            }
        }

        String description = param.getDescription();
        if (description != null) {
            obj.put("description", description);
        }

        String placeHolder = param.getPlaceHolder();
        if (placeHolder != null) {
            obj.put("placeholder", placeHolder);
        }

        return obj;
    }
}
