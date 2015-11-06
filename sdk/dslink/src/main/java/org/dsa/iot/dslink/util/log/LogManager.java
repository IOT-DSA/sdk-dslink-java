package org.dsa.iot.dslink.util.log;

import java.io.File;

/**
 * Utility for setting the default log level for all loggers.
 *
 * @author Samuel Grenier
 */
public class LogManager {

    private static volatile LogBridge instance;

    /**
     * Replaces the default log manager with a custom log manager. This
     * allows for use with custom slf4j back ends.
     *
     * @param logBridge Custom log manager to set.
     */
    public static void setBridge(LogBridge logBridge) {
        if (logBridge == null) {
            throw new NullPointerException("logBridge");
        }

        instance = logBridge;
    }

    private static LogBridge getBridge() {
        if (instance == null) {
            setBridge(new LoggingBridge());
        }
        return instance;
    }

    public static void setLevel(String level) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        level = level.toLowerCase();
        switch (level) {
            case "none":
                setLevel(LogLevel.OFF);
                break;
            case "error":
                setLevel(LogLevel.ERROR);
                break;
            case "warn":
                setLevel(LogLevel.WARN);
                break;
            case "info":
                setLevel(LogLevel.INFO);
                break;
            case "debug":
                setLevel(LogLevel.DEBUG);
                break;
            case "trace":
                setLevel(LogLevel.TRACE);
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
    public static void setLevel(LogLevel level) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        getBridge().setLevel(level);
    }

    /**
     * Configures the root logger with a different layout.
     *
     * @param logPath Path where the logger should output to or {@code null} to
     *                log to standard output steams.
     */
    public static void configure(File logPath) {
        getBridge().configure(logPath);
    }

    /**
     * Retrieves the root logging level, which may also be the global
     * root level.
     *
     * @return Root logger level
     */
    public static LogLevel getLevel() {
        return getBridge().getLevel();
    }

    /**
     * Protected to allow sub-classes to call the constructor but don't
     * permit outsiders to construct LogManager instances.
     */
    protected LogManager() {
    }
}
