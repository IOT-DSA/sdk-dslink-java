package org.dsa.iot.dslink.provider.netty;

import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.Objects;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultLoopProvider extends LoopProvider {
    @Override
    public void schedule(Runnable task) {
        Objects.getDaemonThreadPool().execute(task);
    }

    @Override
    public ScheduledFuture schedule(Runnable task, long delay, TimeUnit timeUnit) {
        return Objects.getDaemonThreadPool().schedule(task, delay, timeUnit);
    }

    @Override
    public ScheduledFuture schedulePeriodic(Runnable task, long initialDelay, long delay, TimeUnit timeUnit) {
        return Objects.getDaemonThreadPool().scheduleWithFixedDelay(task, initialDelay, delay, timeUnit);
    }
}
