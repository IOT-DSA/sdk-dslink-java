package org.dsa.iot.dslink.util;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public interface Client {
    
    void write(JsonObject obj);
    
    RequestTracker getRequestTracker();
    
    ResponseTracker getResponseTracker();
}
