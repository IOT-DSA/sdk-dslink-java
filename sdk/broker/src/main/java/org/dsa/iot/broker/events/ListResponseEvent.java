package org.dsa.iot.broker.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.dsa.iot.core.event.Event;

/**
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public class ListResponseEvent extends Event {
    
    private final int gid;
    
    @Setter
    private String prefix = null;
    
}
