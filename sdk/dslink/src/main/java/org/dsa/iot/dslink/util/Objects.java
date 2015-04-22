package org.dsa.iot.dslink.util;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

import java.util.concurrent.*;

/**
 * Miscellaneous global fields.
 *
 * @author Samuel Grenier
 */
public class Objects {

    private static final int PROCESSORS;
    private static final Vertx VERTX;

    private static volatile ScheduledThreadPoolExecutor THREAD_POOL;
    private static volatile ScheduledThreadPoolExecutor DAEMON_THREAD_POOL;

    public static Vertx getVertx() {
        return VERTX;
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        if (THREAD_POOL == null) {
            THREAD_POOL = new ScheduledThreadPool(PROCESSORS);
        }
        return THREAD_POOL;
    }

    public static void setThreadPool(ScheduledThreadPoolExecutor stpe) {
        THREAD_POOL = stpe;
    }

    public static ScheduledThreadPoolExecutor getDaemonThreadPool() {
        if (DAEMON_THREAD_POOL == null) {
            ThreadFactory factory = getDaemonFactory();
            DAEMON_THREAD_POOL = new ScheduledThreadPool(PROCESSORS, factory);
        }
        return DAEMON_THREAD_POOL;
    }

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

    private static class ScheduledThreadPool extends ScheduledThreadPoolExecutor {

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
                } catch (CancellationException | TimeoutException ignored) {
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    }
                    throw new RuntimeException(e.getCause());
                }
            } else if (t != null) {
                throw new RuntimeException(t);
            }
        }
    }

    static {
        VERTX = VertxFactory.newVertx();
        PROCESSORS = Runtime.getRuntime().availableProcessors();
    }
}
