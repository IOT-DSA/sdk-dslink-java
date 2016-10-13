package org.dsa.iot.shared;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.*;

/**
 * @author Samuel Grenier
 */
public class SharedObjects {
    private static final int MAX_CORE_POOL_SIZE = 64;
    private static final int MIN_CORE_POOL_SIZE = 16;
    private static final int CORE_POOL_FEW_PROCESSORS_THRESHOLD = 2;
    private static final int CORE_POOL_MANY_PROCESSORS_MULTIPLIER = 8;
    
    private static int calculateCorePoolSize() {
        int definedSize = Integer.parseInt(System.getProperty("dsa.shared.threadPoolSize", "0"));
        if (definedSize == 0) {
            int processors = Runtime.getRuntime().availableProcessors();
            if (processors <= CORE_POOL_FEW_PROCESSORS_THRESHOLD) {
                definedSize = MIN_CORE_POOL_SIZE;
            } else {
                definedSize = Math.min(MAX_CORE_POOL_SIZE, processors * CORE_POOL_MANY_PROCESSORS_MULTIPLIER);
            }
        }
        return definedSize;
    }

    public static final int POOL_SIZE = calculateCorePoolSize();

    private static volatile ScheduledThreadPoolExecutor THREAD_POOL;
    private static volatile ScheduledThreadPoolExecutor DAEMON_THREAD_POOL;
    private static volatile EventLoopGroup LOOP;

    public static EventLoopGroup getLoop() {
        if (LOOP == null) {
            LOOP = new NioEventLoopGroup();
        }
        return LOOP;
    }

    public static void setLoop(EventLoopGroup group) {
        LOOP = group;
    }

    public static ScheduledThreadPoolExecutor createThreadPool(int size) {
        return new ScheduledThreadPool(size);
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        if (THREAD_POOL == null) {
            setThreadPool(createThreadPool(POOL_SIZE));
        }
        return THREAD_POOL;
    }

    public static void setThreadPool(ScheduledThreadPoolExecutor stpe) {
        THREAD_POOL = stpe;
    }

    public static ScheduledThreadPoolExecutor createDaemonThreadPool(int size) {
        ThreadFactory factory = getDaemonFactory();
        return new ScheduledThreadPool(size, factory);
    }

    public static ScheduledThreadPoolExecutor getDaemonThreadPool() {
        if (DAEMON_THREAD_POOL == null) {
            setDaemonThreadPool(createDaemonThreadPool(POOL_SIZE));
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

    protected static class ScheduledThreadPool extends ScheduledThreadPoolExecutor {

        public ScheduledThreadPool(int corePoolSize) {
            super(corePoolSize);
            configurePolicies();
        }

        public ScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
            configurePolicies();
        }

        private void configurePolicies() {
            setRemoveOnCancelPolicy(true);
            setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
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
}
