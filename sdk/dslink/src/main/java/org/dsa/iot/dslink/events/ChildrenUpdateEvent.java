package org.dsa.iot.dslink.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.node.Node;

/**
 * Posted when a the children of a parent was updated.
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class ChildrenUpdateEvent extends Event {

    @NonNull
    private final Node parent;

    private final boolean removed;

    private final int rid;
}
