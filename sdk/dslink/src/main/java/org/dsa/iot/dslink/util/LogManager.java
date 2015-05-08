package org.dsa.iot.dslink.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;

/**
 * Utility for setting the default log level for all loggers.
 *
 * @author Samuel Grenier
 */
public class LogManager {

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
            case "trace":
                setLevel(Level.TRACE);
                break;
            default:
                throw new RuntimeException("Unknown log level: " + level);
        }
    }

    /**
     * Configures the root logger with a different layout.
     */
    public static void configure() {
        Logger logger = getLogger();
        LoggerContext loggerContext = logger.getLoggerContext();
        loggerContext.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{32} - %msg%n");
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.start();

        logger.addAppender(appender);
    }

    /**
     * Sets the global logging level
     *
     * @param level Level to set
     */
    public static void setLevel(Level level) {
        if (level == null)
            throw new NullPointerException("level");
        getLogger().setLevel(level);
    }

    /**
     * Retrieves the root logging level, which may also be the global
     * root level.
     *
     * @return Root logger level
     */
    public static Level getLevel() {
        return getLogger().getLevel();
    }

    private static Logger getLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
