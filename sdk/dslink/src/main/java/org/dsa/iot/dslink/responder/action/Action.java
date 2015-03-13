package org.dsa.iot.dslink.responder.action;

import lombok.*;
import org.dsa.iot.dslink.util.Permission;
import org.dsa.iot.dslink.util.StreamState;
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

    @Getter private final String name;
    @NonNull private final Permission permission;
    @NonNull private final Handler<Container> handler;

    public void addParameter(Parameter parameter) {
        params.add(parameter);
    }
    
    public void addResult(Parameter parameter) {
        results.add(parameter);
    }
    
    public void invoke(Container container) {
        if (!hasPermission()) return;
        handler.handle(container);
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

    @Getter
    @RequiredArgsConstructor
    public static class Container {

        /**
         * Data object to act upon for the invocation.
         */
        @NonNull private final JsonObject obj;

        /**
         * Stream state can be set to prevent a closing stream after invocation.
         * The default state is to immediately close the invocation response.
         */
        @NonNull @Setter private StreamState state = StreamState.CLOSED;
    }
}
