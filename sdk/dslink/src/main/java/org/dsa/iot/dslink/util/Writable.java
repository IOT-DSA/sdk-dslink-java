package org.dsa.iot.dslink.util;

import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public interface Writable {
    
    void write(JsonObject obj);
}
