package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public abstract class Database {

    private static final ScheduledThreadPoolExecutor STPE;

    private final Logger logger;

    private final Object connectedLock = new Object();
    private boolean connected;
    private int delay = 1;
    private boolean running;

    public Database(String name) {
        name = getClass().getName() + "::" + name;
        logger = LoggerFactory.getLogger(name);
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Performs a connection to the database supporting reconnecting if a
     * connection gets dropped.
     *
     * @param onConnected Called when the database is connected.
     */
    public void connect(Handler<Database> onConnected) {
        synchronized (connectedLock) {
            running = true;
            if (connected) {
                onConnected.handle(this);
            } else {
                try {
                    performConnect();
                    connected = true;
                    delay = 1;
                    onConnected.handle(this);
                } catch (Exception e) {
                    reconnect(onConnected);
                }
            }
        }
    }

    private void reconnect(final Handler<Database> onConnect) {
        logger.info("Reconnecting in {} seconds", delay);
        STPE.schedule(new Runnable() {
            @Override
            public void run() {
                synchronized (connectedLock) {
                    if (!running) {
                        return;
                    }
                    connect(onConnect);

                    delay *= 2;
                    int cap = 60;
                    if (delay > cap) {
                        delay = cap;
                    }
                }
            }
        }, delay, TimeUnit.SECONDS);

    }

    /**
     * Performs a raw connection to the database endpoint.
     *
     * @throws Exception Connection failed.
     */
    protected abstract void performConnect() throws Exception;

    /**
     * Closes the database connection and frees any resources.
     *
     * @throws Exception Convenience for a failed close.
     */
    protected abstract void close() throws Exception;

    /**
     * Initialize additional top level database data.
     *
     * @param node Node to initialize data, usually database node.
     */
    public abstract void initExtensions(Node node);

    static {
        STPE = Objects.createDaemonThreadPool();
    }
}
