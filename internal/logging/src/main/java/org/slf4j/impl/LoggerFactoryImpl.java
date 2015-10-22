package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Samuel Grenier
 */
public class LoggerFactoryImpl implements ILoggerFactory {

    private Level logLevel = Level.INFO;
    private PrintStream stream = System.out;
    private boolean shouldClose = false;

    public LoggerFactoryImpl() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (!shouldClose) {
                    return;
                }
                stream.close();
            }
        }));
    }

    public void setLogPath(File logPath) {
        if (logPath == null) {
            shouldClose = false;
            stream = System.out;
            return;
        }

        try {
            if (shouldClose) {
                stream.close();
            }
            shouldClose = true;
            boolean exists = logPath.exists();
            OutputStream out = new FileOutputStream(logPath, exists);
            stream = new PrintStream(out, false, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PrintStream getPrintStream() {
        return stream;
    }

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
