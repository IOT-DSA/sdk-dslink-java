package org.dsa.iot.dslink.util.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.impl.Level;
import org.slf4j.impl.LoggerFactoryImpl;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.File;

/**
 * @author Samuel Grenier
 */
public class LoggingBridge implements LogBridge {

    private LogLevel level;

    @Override
    public void configure(File path) {
        getLoggerFactory().setLogPath(path);
    }

    @Override
    public void setLevel(LogLevel level) {
        LoggerFactoryImpl logger = getLoggerFactory();
        switch (level) {
            case OFF:
                logger.setLogLevel(Level.OFF);
                break;
            case ERROR:
                logger.setLogLevel(Level.ERROR);
                break;
            case WARN:
                logger.setLogLevel(Level.WARN);
                break;
            case INFO:
                logger.setLogLevel(Level.INFO);
                break;
            case DEBUG:
                logger.setLogLevel(Level.DEBUG);
                break;
            case TRACE:
                logger.setLogLevel(Level.TRACE);
        }
        this.level = level;
    }

    @Override
    public LogLevel getLevel() {
        return level;
    }

    private LoggerFactoryImpl getLoggerFactory() {
        StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
        ILoggerFactory factory = binder.getLoggerFactory();
        return (LoggerFactoryImpl) factory;
    }
}
