package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * @author Samuel Grenier
 */
@SuppressWarnings("unused")
public class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final StaticLoggerBinder INSTANCE = new StaticLoggerBinder();

    private final LoggerFactoryImpl factory;
    private final String className;

    public static StaticLoggerBinder getSingleton() {
        return INSTANCE;
    }

    private StaticLoggerBinder() {
        factory = new LoggerFactoryImpl();
        className = LoggerFactoryImpl.class.getName();
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return factory;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return className;
    }
}
