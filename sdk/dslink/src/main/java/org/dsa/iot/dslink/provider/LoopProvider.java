package org.dsa.iot.dslink.provider;

import org.dsa.iot.dslink.provider.netty.DefaultLoopProvider;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Allows customization of the event loop that the SDK will use.
 */
public abstract class LoopProvider {
    private static LoopProvider PROVIDER;

    /**
     * Gets the current event loop provider.
     * @return current event loop provider
     */
    public static LoopProvider getProvider() {
        if (PROVIDER == null) {
            setProvider(new DefaultLoopProvider());
        }
        return PROVIDER;
    }

    /**
     * Sets the current event loop provider.
     * @param provider event loop provider
     */
    public static void setProvider(LoopProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        PROVIDER = provider;
    }

    /**
     * Schedule a task on the event loop immediately.
     * @param task a task to schedule
     */
    public abstract void schedule(Runnable task);

    /**
     * Schedule a task on the event loop to run after a delay.
     * @param task a task to schedule
     * @param delay delay time
     * @param timeUnit delay time unit
     * @return a future for this task
     */
    public abstract ScheduledFuture schedule(Runnable task, long delay, TimeUnit timeUnit);

    /**
     * Schedule a task on the event loop periodically.
     * @param task a task to schedule
     * @param initialDelay initial delay time
     * @param delay periodic delay
     * @param timeUnit delay time unit
     * @return a future for this task
     */
    public abstract ScheduledFuture schedulePeriodic(Runnable task, long initialDelay, long delay, TimeUnit timeUnit);
}
