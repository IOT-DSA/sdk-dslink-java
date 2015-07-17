package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author Samuel Grenier
 */
public class LoggerFactoryImpl implements ILoggerFactory {

    private Level logLevel = Level.INFO;

    public void setLogLevel(Level level) {
        this.logLevel = level;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    @Override
    public Logger getLogger(String name) {
        return new LoggerImpl(this, name);
    }
}
