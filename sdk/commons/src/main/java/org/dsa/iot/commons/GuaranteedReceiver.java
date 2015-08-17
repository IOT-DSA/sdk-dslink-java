package org.dsa.iot.commons;

import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The {@link GuaranteedReceiver} is designed to guarantee retrieving an
 * instance of {@link T}. The instance will be instantiated as necessary.
 *
 * @author Samuel Grenier
 */
public abstract class GuaranteedReceiver<T> {

    private static final Logger LOGGER;

    private final TimeUnit timeUnit;
    private final long delay;

    private final List<Handler<T>> list = new ArrayList<>();
    private ScheduledFuture<?> fut;
    private T instance;

    private boolean running = true;

    /**
     *
     * @param delay Delay, in seconds, to instantiate the instance if an
     *              error occurs.
     * @see #GuaranteedReceiver(long, TimeUnit)
     */
    public GuaranteedReceiver(long delay) {
        this(delay, TimeUnit.SECONDS);
    }

    /**
     * The instance of {@link T} will not be initially initialized until
     * {@link #get} is called.
     *
     * @param delay Delay to instantiate the instance, in the specified
     *              {@link TimeUnit}, to instantiate the instance if an
     *              error occurs.
     * @param unit Unit of time the delay is in.
     */
    public GuaranteedReceiver(long delay, TimeUnit unit) {
        this.delay = delay;
        this.timeUnit = unit;
    }

    /**
     * Creates the instance of {@link T}.
     *
     * @return The created instance of {@link T}
     * @throws Exception An error occurred creating the instance.
     */
    protected abstract T instantiate() throws Exception;

    /**
     * If the instance is invalidated, then it will be set to {@code null} and
     * be re-instantiated, otherwise the current instance of {@link T} will
     * not be set to {@code null}.
     *
     * @param e Exception that occurred when instantiating or calling a
     *          handler.
     * @return  Whether to invalidate the instance or not. It is recommended to
     *          return {@code false} by default.
     */
    protected abstract boolean invalidateInstance(Exception e);

    /**
     * Guarantees the instance of {@link T} is available for consumption.
     *
     * @param handler Called when the instance has been retrieved and is ready
     *                for consumption.
     * @param checked If the receiver is shutdown and {@code checked} is
     *                {@code true} then {@link IllegalStateException} is
     *                thrown, otherwise the {@code handler} gets ignored.
     */
    public final void get(final Handler<T> handler, boolean checked) {
        boolean reattempt = false;
        synchronized (this) {
            if (!running) {
                if (checked) {
                    throw new IllegalStateException("Receiver shutdown");
                } else {
                    return;
                }
            }

            if (instance == null) {
                if (handler != null) {
                    list.add(handler);
                }
                if (fut != null) {
                    return;
                }
                ScheduledThreadPoolExecutor stpe = getSTPE();
                InstantiationRunner runner = new InstantiationRunner();
                fut = stpe.scheduleWithFixedDelay(runner, 0, delay, timeUnit);
            } else if (handler != null) {
                try {
                    handler.handle(instance);
                } catch (Exception e) {
                    if (invalidateInstance(e)) {
                        instance = null;
                        reattempt = true;
                    } else {
                        LOGGER.error("Unhandled exception", e);
                    }
                }
            }
        }
        if (reattempt) {
            get(handler, checked);
        }
    }

    /**
     * Shuts down the receiver from preventing any new instances to be created.
     *
     * @return The underlying instance of {@link T}, which can be
     * {@code null}, to allow freeing any resources the instance holds.
     */
    public synchronized T shutdown() {
        running = false;
        stop();
        T tmp = instance;
        instance = null;
        return tmp;
    }

    /**
     * Cancels the scheduled future from running any further.
     */
    private synchronized void stop() {
        if (fut != null) {
            try {
                fut.cancel(true);
            } catch (Exception ignored) {
            }
            fut = null;
        }
    }

    private class InstantiationRunner implements Runnable {
        @Override
        public void run() {
            synchronized (GuaranteedReceiver.this) {
                if (!running) {
                    return;
                }

                try {
                    instance = instantiate();
                    stop();
                } catch (Exception e) {
                    LOGGER.debug("Failed to instantiate", e);
                    return;
                }
            }

            List<Handler<T>> list;
            synchronized (GuaranteedReceiver.this) {
                list = new ArrayList<>(GuaranteedReceiver.this.list);
            }

            ScheduledThreadPoolExecutor stpe = getSTPE();
            final Boolean doBreak = new Boolean();
            for (final Handler<T> handler : list) {
                // latch is used to ensure the instance check is complete
                final CountDownLatch latch = new CountDownLatch(1);
                stpe.execute(new Runnable() {
                    @Override
                    public void run() {
                        boolean doRemove = true;
                        try {
                            T inst;
                            synchronized (GuaranteedReceiver.this) {
                                inst = instance;
                                if (inst == null) {
                                    doBreak.value = true;
                                    latch.countDown();
                                    return;
                                }
                                latch.countDown();
                            }
                            handler.handle(inst);
                        } catch (Exception e) {
                            if (invalidateInstance(e)) {
                                synchronized (GuaranteedReceiver.this) {
                                    instance = null;
                                }
                                doRemove = false;
                                get(null, false);
                            } else {
                                LOGGER.error("Unhandled exception", e);
                            }
                        } finally {
                            if (doRemove) {
                                synchronized (GuaranteedReceiver.this) {
                                    GuaranteedReceiver.this.list.remove(handler);
                                }
                            }
                        }
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (doBreak.value) {
                    break;
                }
            }
        }
    }

    private static ScheduledThreadPoolExecutor getSTPE() {
        return Objects.getDaemonThreadPool();
    }

    private static class Boolean {
        boolean value;
    }

    static {
        LOGGER = LoggerFactory.getLogger(GuaranteedReceiver.class);
    }
}
