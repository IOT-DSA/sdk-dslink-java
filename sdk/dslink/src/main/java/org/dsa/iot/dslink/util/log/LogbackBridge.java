package org.dsa.iot.dslink.util.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class LogbackBridge implements LogBridge {

    private LogLevel level;

    @Override
    public void configure() {
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

    @Override
    public void setLevel(LogLevel level) {
        Logger logger = getLogger();
        switch (level) {
            case OFF:
                logger.setLevel(Level.OFF);
                break;
            case ERROR:
                logger.setLevel(Level.ERROR);
                break;
            case WARN:
                logger.setLevel(Level.WARN);
                break;
            case INFO:
                logger.setLevel(Level.INFO);
                break;
            case DEBUG:
                logger.setLevel(Level.DEBUG);
                break;
            case TRACE:
                logger.setLevel(Level.TRACE);
        }
        this.level = level;
    }

    public LogLevel getLevel() {
        return level;
    }

    private Logger getLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
