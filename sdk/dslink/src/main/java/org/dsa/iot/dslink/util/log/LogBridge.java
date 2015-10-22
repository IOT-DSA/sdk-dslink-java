package org.dsa.iot.dslink.util.log;

import java.io.File;

/**
 * @author Samuel Grenier
 */
public interface LogBridge {

    /**
     * Configures the logger.
     *
     * @param logPath Path where the logger should output to or {@code null} to
     *                log to standard output steams.
     */
    void configure(File logPath);

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
