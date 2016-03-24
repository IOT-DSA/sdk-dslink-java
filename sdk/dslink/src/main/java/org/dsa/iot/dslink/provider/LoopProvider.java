package org.dsa.iot.dslink.provider;

import org.dsa.iot.dslink.provider.netty.DefaultLoopProvider;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class LoopProvider {
    private static LoopProvider PROVIDER;

    public static LoopProvider getProvider() {
        if (PROVIDER == null) {
            setProvider(new DefaultLoopProvider());
        }
        return PROVIDER;
    }

    public static void setProvider(LoopProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        PROVIDER = provider;
    }

    public abstract void schedule(Runnable task);
    public abstract ScheduledFuture schedule(Runnable task, long time, TimeUnit timeUnit);
    public abstract ScheduledFuture schedule(Runnable task, long initialDelay, long delay, TimeUnit timeUnit);
}
