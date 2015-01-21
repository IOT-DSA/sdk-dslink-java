package org.dsa.iot.responder.methods;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.responder.node.Node;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class InvokeMethod extends Method {

    @NonNull
    private final Node node;

    @Override
    public JsonObject invoke(JsonObject request) {
        if (node.isInvokable()) {
            Handler<Void> handler = node.getInvocationHandler();
            handler.handle(null);
        } else {
            throw new RuntimeException("Not invokable");
        }
        setState(StreamState.CLOSED);
        return null;
    }
}
