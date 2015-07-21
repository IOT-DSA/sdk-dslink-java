package org.dsa.iot.dslink.util;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.DefaultVertx;

import java.util.concurrent.*;

/**
 * Miscellaneous global fields.
 *
 * @author Samuel Grenier
 */
public class Objects {

    private static final int POOL_SIZE = 32;
    private static Vertx VERTX;

    private static volatile ScheduledThreadPoolExecutor THREAD_POOL;
    private static volatile ScheduledThreadPoolExecutor DAEMON_THREAD_POOL;

    public static Vertx getVertx() {
        return VERTX;
    }

    public static ScheduledThreadPoolExecutor createThreadPool() {
        return createThreadPool(POOL_SIZE);
    }

    public static ScheduledThreadPoolExecutor createThreadPool(int size) {
        return new ScheduledThreadPool(size);
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        if (THREAD_POOL == null) {
            THREAD_POOL = createThreadPool();
        }
        return THREAD_POOL;
    }

    @SuppressWarnings("unused")
    public static void setThreadPool(ScheduledThreadPoolExecutor stpe) {
        THREAD_POOL = stpe;
    }

    public static ScheduledThreadPoolExecutor createDaemonThreadPool() {
        return createDaemonThreadPool(POOL_SIZE);
    }

    public static ScheduledThreadPoolExecutor createDaemonThreadPool(int size) {
        ThreadFactory factory = getDaemonFactory();
        return new ScheduledThreadPool(size, factory);
    }

    public static ScheduledThreadPoolExecutor getDaemonThreadPool() {
        if (DAEMON_THREAD_POOL == null) {
            DAEMON_THREAD_POOL = createDaemonThreadPool();
        }
        return DAEMON_THREAD_POOL;
    }

    @SuppressWarnings("unused")
    public static void setDaemonThreadPool(ScheduledThreadPoolExecutor stpe) {
        DAEMON_THREAD_POOL = stpe;
    }

    private static ThreadFactory getDaemonFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        };
    }

    protected static class ScheduledThreadPool extends ScheduledThreadPoolExecutor {

        public ScheduledThreadPool(int corePoolSize) {
            super(corePoolSize);
            setRemoveOnCancelPolicy(true);
        }

        public ScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
            setRemoveOnCancelPolicy(true);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable t) {
            if (t == null && runnable instanceof Future<?>) {
                try {
                    ((Future<?>) runnable).get(0, TimeUnit.NANOSECONDS);
                } catch (CancellationException
                        | InterruptedException
                        | TimeoutException ignored) {
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    }
                    throw new RuntimeException(e.getCause());
                }
            } else if (t != null) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                throw new RuntimeException(t);
            }
        }
    }

    static {
        VERTX = new DefaultVertx();
    }
}
