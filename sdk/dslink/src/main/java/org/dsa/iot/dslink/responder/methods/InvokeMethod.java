package org.dsa.iot.dslink.responder.methods;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@AllArgsConstructor
public class InvokeMethod extends Method {

    @NonNull
    private final Node node;

    @Override
    public JsonArray invoke(JsonObject request) {
        if (node.getInvokable() != null) {
            Handler<Void> handler = node.getInvocationHandler();
            handler.handle(null);
        } else {
            throw new RuntimeException("Not invokable");
        }
        setState(StreamState.CLOSED);
        return null;
    }
}
