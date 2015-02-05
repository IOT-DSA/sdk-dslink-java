package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class IncomingDataEvent extends Event {

    @NonNull
    private final JsonObject data;

}
