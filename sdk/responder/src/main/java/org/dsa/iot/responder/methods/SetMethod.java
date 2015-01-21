package org.dsa.iot.responder.methods;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.responder.node.Node;
import org.dsa.iot.responder.node.value.Value;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class SetMethod extends Method {

    @NonNull
    private final Node node;

    /**
     * References a configuration or attribute. Can be null to reference
     * the node itself.
     */
    private final String specialPath;

    @Override
    public JsonObject invoke(JsonObject request) {
        Object obj = request.getField("value");

        Value value;
        if (obj instanceof Number) {
            value = new Value(((Number) obj).intValue());
        } else if (obj instanceof Boolean) {
            value = new Value((Boolean) obj);
        } else if (obj instanceof String) {
            value = new Value((String) obj);
        } else {
            throw new RuntimeException("Unhandled type or null (" + (obj == null) + ")");
        }

        boolean config = false;
        String name = null;
        if (specialPath != null)
            name = specialPath.substring(1);
        if (name != null && specialPath.startsWith("$"))
            config = true;
        else if (name != null && specialPath.startsWith("@"))
            config = false;
        else if (name != null)
            throw new RuntimeException("Invalid node prefix: " + specialPath);

        if (name != null && config)
            node.setConfiguration(name, value);
        else if (name != null)
            node.setAttribute(name, value);
        else
            node.setValue(value, false);

        setState(StreamState.CLOSED);
        return null;
    }
}
