package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.requester.RequestTracker;
import org.dsa.iot.dslink.responder.ResponseTracker;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public interface Client {
    
    boolean write(JsonObject obj);

    RequestTracker getRequestTracker();
    
    ResponseTracker getResponseTracker();
}
