package org.dsa.iot.dslink.node.actions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Samuel Grenier
 */
public class ActionRegistry {

    private final Map<String, Action> actions = new ConcurrentHashMap<>();

    /**
     * Registers an action to the registry.
     *
     * @param name Name of the action, must be unique.
     * @param action Action to register
     */
    public void register(String name, Action action) {
        Action prev = actions.put(name, action);
        if (prev != null) {
            throw new RuntimeException(name + " has already been registered");
        }
    }

    /**
     * Retrieves a registered action and sets it on the node.
     *
     * @param name Name of the action
     * @return Registered action or null if it doesn't exist
     */
    public Action getAction(String name) {
        return actions.get(name);
    }
}
