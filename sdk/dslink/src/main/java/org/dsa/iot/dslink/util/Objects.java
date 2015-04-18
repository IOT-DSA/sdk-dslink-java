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

    private static final Vertx VERTX;
    private static final ScheduledThreadPoolExecutor THREAD_POOL;
    private static final ScheduledThreadPoolExecutor DAEMON_THREAD_POOL;

    public static Vertx getVertx() {
        return VERTX;
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        return THREAD_POOL;
    }

    public static ScheduledThreadPoolExecutor getDaemonThreadPool() {
        return DAEMON_THREAD_POOL;
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

        int p = Runtime.getRuntime().availableProcessors();
        THREAD_POOL = new ScheduledThreadPool(p);
        DAEMON_THREAD_POOL = new ScheduledThreadPool(p, getDaemonFactory());
    }
}
