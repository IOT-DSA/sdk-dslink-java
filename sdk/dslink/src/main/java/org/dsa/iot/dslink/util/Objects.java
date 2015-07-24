package org.dsa.iot.dslink.util;

import org.dsa.iot.shared.SharedObjects;
import org.vertx.java.core.Vertx;

import java.util.concurrent.*;

/**
 * Miscellaneous global fields.
 *
 * @author Samuel Grenier
 */
public class Objects {

    private static volatile ScheduledThreadPoolExecutor THREAD_POOL;
    private static volatile ScheduledThreadPoolExecutor DAEMON_THREAD_POOL;

    public static Vertx getVertx() {
        return SharedObjects.getVertx();
    }

    @SuppressWarnings("unused")
    public static ScheduledThreadPoolExecutor createThreadPool() {
        return createThreadPool(SharedObjects.POOL_SIZE);
    }

    public static ScheduledThreadPoolExecutor createThreadPool(int size) {
        return SharedObjects.createThreadPool(size);
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        if (THREAD_POOL == null) {
            THREAD_POOL = SharedObjects.getThreadPool();
        }
        return THREAD_POOL;
    }

    @SuppressWarnings("unused")
    public static void setThreadPool(ScheduledThreadPoolExecutor stpe) {
        THREAD_POOL = stpe;
    }

    public static ScheduledThreadPoolExecutor createDaemonThreadPool() {
        return createDaemonThreadPool(SharedObjects.POOL_SIZE);
    }

    public static ScheduledThreadPoolExecutor createDaemonThreadPool(int size) {
        return SharedObjects.createDaemonThreadPool(size);
    }

    public static ScheduledThreadPoolExecutor getDaemonThreadPool() {
        if (DAEMON_THREAD_POOL == null) {
            DAEMON_THREAD_POOL = SharedObjects.getDaemonThreadPool();
        }
        return DAEMON_THREAD_POOL;
    }

    @SuppressWarnings("unused")
    public static void setDaemonThreadPool(ScheduledThreadPoolExecutor stpe) {
        DAEMON_THREAD_POOL = stpe;
    }
}
