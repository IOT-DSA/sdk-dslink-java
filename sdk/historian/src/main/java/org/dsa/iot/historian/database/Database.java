package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.handler.CompleteHandler;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.historian.utils.QueryData;
import org.dsa.iot.historian.utils.TimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public abstract class Database {

    private static final ScheduledThreadPoolExecutor STPE;

    private final DatabaseProvider provider;
    private final Logger logger;

    private final Object connectedLock = new Object();
    private boolean connected;
    private int delay = 1;
    private boolean running;

    public Database(String name, DatabaseProvider provider) {
        this.provider = provider;

        logger = LoggerFactory.getLogger(name);
    }

    public DatabaseProvider getProvider() {
        return provider;
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
     * Values must be written into the database in UTC.
     *
     * @param path Path to query.
     * @param value Value received from the server.
     * @param ts Converted time in UTC.
     * @see TimeParser
     */
    public abstract void write(String path, Value value, long ts);

    /**
     * Times must be in UTC. At the end of the query, the {@code handler} must
     * receive a {@code null} event in order to close the table stream.
     *
     * @param path Path to query.
     * @param from Beginning search time.
     * @param to End search time.
     * @param handler Handler callback for incoming data.
     */
    public abstract void query(String path,
                               long from,
                               long to,
                               CompleteHandler<QueryData> handler);

    /**
     * @param path Path to query.
     * @return The first value stored in the database.
     */
    public abstract QueryData queryFirst(String path);

    /**
     * @param path Path to query.
     * @return The last value stored in the database.
     */
    public abstract QueryData queryLast(String path);

    /**
     * Closes the database connection and frees any resources.
     *
     * @throws Exception Convenience for a failed close.
     */
    public abstract void close() throws Exception;

    /**
     * Performs a raw connection to the database endpoint.
     *
     * @throws Exception Connection failed.
     */
    protected abstract void performConnect() throws Exception;

    /**
     * Initialize additional top level database data and settings. All
     * settings should be stored in the "edit" action for consistency.
     *
     * @param node Node to initialize data, usually database node.
     * @see DatabaseProvider#dbPermission() For action permission level.
     */
    public abstract void initExtensions(Node node);

    static {
        STPE = Objects.createDaemonThreadPool();
    }
}
