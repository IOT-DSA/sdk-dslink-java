package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.responder.action.ActionRegistry;

/**
 * Allows populating a {@link org.dsa.iot.dslink.DSLink} instance before it is
 * fully initialized. Due to serialization, this is strictly for data population
 * of the action registry.
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class InitializationEvent extends Event {

    @NonNull private final DSLink link;
    private final ActionRegistry actionRegistry;

}
