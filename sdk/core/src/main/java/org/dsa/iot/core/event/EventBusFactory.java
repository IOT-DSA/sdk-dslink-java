package org.dsa.iot.core.event;

import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.common.Properties;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;

/**
 * @author Samuel Grenier
 */
public class EventBusFactory {
    
    public static MBassador<Event> create() {
        val config = new BusConfiguration();
        config.addFeature(Feature.SyncPubSub.Default());
        config.addFeature(Feature.AsynchronousHandlerInvocation.Default());
        config.addFeature(Feature.AsynchronousMessageDispatch.Default());

        val logger = new IPublicationErrorHandler.ConsoleLogger();
        config.setProperty(Properties.Handler.PublicationError, logger);

        return new MBassador<>(config);
    }
}
