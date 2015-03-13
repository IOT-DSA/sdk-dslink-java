package org.dsa.iot.dslink.responder.methods;

import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.node.Node;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.dsa.iot.dslink.responder.action.Action.Container;

/**
 * @author Samuel Grenier
 */
public class InvokeMethod extends Method {

    @NonNull private final Node node;
    
    public InvokeMethod(Node node,
                        JsonObject request) {
        super(request);
        this.node = node;
    }

    @Override
    public JsonArray invoke() {
        val handler = node.getAction();
        val container = new Container(getRequest());
        if (handler != null && handler.hasPermission()) {
            handler.invoke(container);
        } else {
            throw new RuntimeException("Not invokable");
        }
        setState(container.getState());
        return container.getUpdates();
    }
}
