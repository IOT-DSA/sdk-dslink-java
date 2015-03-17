package org.dsa.iot.dslink.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for setting the default log level for all loggers.
 * @author Samuel Grenier
 */
public class LogLevel {

    public static void setLevel(Level level) {
        Logger log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        log.setLevel(Level.INFO);
    }

}
