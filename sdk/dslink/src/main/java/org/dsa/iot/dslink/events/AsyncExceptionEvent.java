package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.core.event.Event;

/**
 * Throws an event when an asynchronous exception occurs with vert.x
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class AsyncExceptionEvent extends Event {

    @NonNull
    private final Throwable throwable;

}
