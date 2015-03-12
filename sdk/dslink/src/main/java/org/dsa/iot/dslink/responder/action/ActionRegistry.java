package org.dsa.iot.dslink.responder.action;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.dsa.iot.dslink.node.exceptions.DuplicateException;
import org.vertx.java.core.json.JsonObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class ActionRegistry {

    private Map<String, Action> actions;

    public synchronized Action get(@NonNull String name) {
        Action action;
        if (actions == null || (action = actions.get(name)) == null)
            throw new RuntimeException("Unknown action: " + name);
        return action;
    }

    @SneakyThrows
    public synchronized Action add(@NonNull Action action) {
        if (actions == null) {
            actions = new HashMap<>();
        }
        val name = action.getName();
        Action act = actions.put(name, action);
        if (act != null) {
            throw new DuplicateException("Action '" + name + "' was already registered");
        }
        return action;
    }

    public synchronized Action remove(@NonNull String name) {
        return actions != null ? actions.remove(name) : null;
    }
}
