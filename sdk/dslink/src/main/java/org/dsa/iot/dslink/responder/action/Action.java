package org.dsa.iot.dslink.responder.action;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.dsa.iot.dslink.util.Permission;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class Action {
    
    private final List<Parameter> params = new ArrayList<>();
    private final List<Parameter> results = new ArrayList<>();
    
    @NonNull private final Permission permission;
    @NonNull private final Handler<JsonObject> handler;

    public void addParameter(Parameter parameter) {
        params.add(parameter);
    }
    
    public void addResult(Parameter parameter) {
        results.add(parameter);
    }
    
    public void invoke(JsonObject obj) {
        if (!hasPermission()) return;
        handler.handle(obj);
    }
    
    public boolean hasPermission() {
        return permission != Permission.NONE;
    }
    
    public void toJson(JsonObject obj) {
        if (!hasPermission()) return;
        obj.putString("$invokable", permission.getJsonName());
        obj.putArray("$params", paramsToJson(params));
        obj.putArray("$columns", paramsToJson(results));
    }
    
    private JsonArray paramsToJson(List<Parameter> p) {
        val array = new JsonArray();
        for (Parameter param : p) {
            val obj = new JsonObject();
            obj.putString("name", param.getName());
            obj.putString("type", param.getType().toJsonString());
            array.add(obj);
        }
        return array;
    }
}
