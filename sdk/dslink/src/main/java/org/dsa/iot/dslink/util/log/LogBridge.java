package org.dsa.iot.dslink.util.log;

/**
 * @author Samuel Grenier
 */
public interface LogBridge {

    /**
     * Configures the logger.
     */
    void configure();

    /**
     * Sets the log level of the root logger.
     *
     * @param level Level to set.
     */
    void setLevel(LogLevel level);

    /**
     *
     * @return Log level of the root logger.
     */
    LogLevel getLevel();
}
