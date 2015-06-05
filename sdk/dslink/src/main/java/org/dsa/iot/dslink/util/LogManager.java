package org.dsa.iot.dslink.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;

/**
 * Utility for setting the default log level for all loggers.
 *
 * @author Samuel Grenier
 */
public class LogManager {

	private static final LogManager DEFAULT_LOG_MANAGER = createDefaultLogManager();
	
    private static LogManager createDefaultLogManager() {
    	LogManager defaultLogManager = new LogManager();
    	return defaultLogManager ;
    }
    
	/** 
	 * Use the {@link #setInstance(LogManager)} method to
	 * customize the active LogManager implementation.
	 */
    private final static AtomicReference<LogManager> refLogManagerImpl = new AtomicReference<>(DEFAULT_LOG_MANAGER);
	
    /**
     * <p>
     * Replace the "default" LogManager implementation
     * with a custom implementation.
     * 
     * <p>
     * Useful if the app container running sdk-dslink-java
     * is using an Simple Logging Facade for Java (SLF4J)
     * backend other than logback.
     * 
     * @param customLogManager In-parameter, a LogManager instance. 
     */
	public static void setInstance( LogManager customLogManager ) {
		
		if (null==customLogManager) {
			throw new IllegalArgumentException("attempt to set null customLogManager");
		}
		
		refLogManagerImpl.set(customLogManager);
    }	
	
    public static void setLevel(String level) {
    	LogManager custom = refLogManagerImpl.get();
    	custom.doSetLevel(level);
    }

	/**
     * Configures the root logger with a different layout.
     */
    public static void configure() {
    	LogManager custom = refLogManagerImpl.get();    	
		custom.doConfigure();
    }

    /**
     * Sets the global logging level
     *
     * @param level Level to set
     */
    public static void setLevel(Level level) {
    	LogManager custom = refLogManagerImpl.get();    	
    	custom.doSetLevel(level);
    }

	/**
     * Retrieves the root logging level, which may also be the global
     * root level.
     *
     * @return Root logger level
     */
    public static Level getLevel() {
    	LogManager custom = refLogManagerImpl.get();
    	return custom.doGetLevel();
    }

    private static Logger getLogger() {
    	LogManager custom = refLogManagerImpl.get();
    	return custom.doGetLogger();
    }

    /**
     * Protected to allow sub-classes to call the constructor but don't
     * permit outsiders to construct LogManager instances.
     */
    protected LogManager() {}
    
    // Method implementations for the "Default" LogManager
    
	protected void doSetLevel(String level) {
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
    
    protected void doConfigure() {
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

	protected void doSetLevel(Level level) {
        if (level == null)
            throw new NullPointerException("level");
        getLogger().setLevel(level);
    }
    
    protected Level doGetLevel() {  
    	return getLogger().getLevel(); 
    }
	
    protected Logger doGetLogger() { 
    	return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME); 
    }	
}
