package org.dsa.iot.core.event;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;

/**
 * @author Samuel Grenier
 */
public class EventBusFactory {
    
    public static MBassador<Event> create() {
        BusConfiguration config = new BusConfiguration();
        config.addFeature(Feature.SyncPubSub.Default());
        config.addFeature(Feature.AsynchronousHandlerInvocation.Default());
        config.addFeature(Feature.AsynchronousMessageDispatch.Default());
        return new MBassador<>(config);
    }
}
