package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Posted when an open stream is closed.
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class ClosedStreamEvent {

    private final int rid;

}
