package org.dsa.iot.dslink.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for setting the default log level for all loggers.
 *
 * @author Samuel Grenier
 */
public class LogLevel {

    public static void setLevel(String level) {
        if (level == null)
            throw new NullPointerException("level");
        level = level.toLowerCase();
        switch (level) {
            case "none":
                setLevel(Level.OFF);
                break;
            case "error":
                setLevel(Level.ERROR);
                break;
            case "warn":
                setLevel(Level.WARN);
                break;
            case "info":
                setLevel(Level.INFO);
                break;
            case "debug":
                setLevel(Level.DEBUG);
                break;
            default:
                throw new RuntimeException("Unknown log level: " + level);
        }
    }

    /**
     * Sets the global logging level
     *
     * @param level Level to set
     */
    public static void setLevel(Level level) {
        if (level == null)
            throw new NullPointerException("level");
        Logger log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        log.setLevel(level);
    }

    /**
     * Retrieves the root logging level, which may also be the global
     * root level.
     *
     * @return Root logger level
     */
    public static Level getLevel() {
        Logger log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        return log.getLevel();
    }
}
