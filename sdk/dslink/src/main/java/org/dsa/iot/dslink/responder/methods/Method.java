package org.dsa.iot.dslink.responder.methods;

import lombok.*;
import org.dsa.iot.dslink.util.StreamState;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public abstract class Method {

    @Setter(AccessLevel.PROTECTED)
    private StreamState state = null;
    
    @NonNull private final JsonObject request;
    
    /**
     * @return An array of update responses
     */
    public abstract JsonArray invoke();

    /**
     * Called after the response was sent to the client. 
     */
    public void postSent() {
        
    }
}
