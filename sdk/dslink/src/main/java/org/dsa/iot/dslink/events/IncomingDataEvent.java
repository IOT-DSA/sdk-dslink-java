package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.util.Writable;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class IncomingDataEvent extends Event {

    @NonNull
    private final Writable client;
    
    @NonNull
    private final JsonObject data;

}
