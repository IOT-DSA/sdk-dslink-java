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
 * A trivial use case is to support automatically reconnecting to servers
 * in an elegant and standard fashion.
 *
 * @author Samuel Grenier
 */
public abstract class GuaranteedReceiver<T> {

    private static final Logger LOGGER;

    private final TimeUnit timeUnit;
    private final long delay;

    private final List<Handler<T>> list = new ArrayList<>();
    private ScheduledFuture<?> instantiationFut;
    private ScheduledFuture<?> loopFut;
    private T instance;

    private boolean running = true;

    /**
     * Constructs a {@link GuaranteedReceiver} with a specified {@code delay}.
     *
     * @param delay Delay, in seconds, to instantiate the instance if an error
     *              occurs.
     * @see #GuaranteedReceiver(long, boolean)
     */
    public GuaranteedReceiver(long delay) {
        this(delay, false);
    }

    /**
     *
     * @param delay Delay, in seconds, to instantiate the instance if an error
     *              occurs.
     * @param loop Whether to enable looping or not.
     * @see #GuaranteedReceiver(long, TimeUnit, boolean)
     */
    public GuaranteedReceiver(long delay, boolean loop) {
        this(delay, TimeUnit.SECONDS, loop);
    }

    /**
     * The instance of {@link T} will not be initially initialized until
     * {@link #get} is called or looping is enabled. If looping is enabled then
     * {@link #onLoop} will be called repeatedly when the {@code delay} time
     * elapses based on the specified {@code unit} of time.
     *
     * @param delay Delay to instantiate the instance, in the specified
     *              {@link TimeUnit}, to instantiate the instance if an
     *              error occurs.
     * @param unit Unit of time the delay is in.
     * @param loop Whether to enable looping or not.
     */
    public GuaranteedReceiver(long delay, TimeUnit unit, boolean loop) {
        if (delay <= 0) {
            String err = "Delay must be greater than zero";
            throw new IllegalArgumentException(err);
        }
        this.delay = delay;
        this.timeUnit = unit;
        if (loop) {
            initializeLoop();
        }
    }

    /**
     * Creates the instance of {@link T}.
     *
     * @return The created instance of {@link T}
     * @throws Exception An error occurred creating the instance.
     */
    protected abstract T instantiate() throws Exception;

    /**
     * If the instance of {@link T} is invalidated, then it will be set to
     * {@code null} and be re-instantiated, otherwise the current instance
     * of {@link T} will not be set to {@code null}. If the instance is not
     * invalidated then it will be treated as an unhandled exception and will
     * be logged.
     *
     * @param e Exception that occurred when instantiating or calling a
     *          handler.
     * @return  Whether to invalidate the instance or not. It is recommended to
     *          return {@code false} by default.
     */
    protected abstract boolean invalidateInstance(Exception e);

    /**
     * Called every time a loop occurs. The looping feature must be enabled. A
     * loop is called based on the desired delay.
     *
     * The loop functionality is designed to test if a resource is still being
     * held to determine a status as needed. A {@link RuntimeException} must
     * be thrown in order to invalidate the instance. The implementation of
     * {@link #invalidateInstance(Exception)} will determine if the instance
     * should be invalidated.
     *
     * @param event Instantiated {@link T}.
     * @see #GuaranteedReceiver(long, TimeUnit, boolean)
     * @see #invalidateInstance(Exception)
     */
    @SuppressWarnings("UnusedParameters")
    protected void onLoop(T event) {
    }

    /**
     * Guarantees the instance of {@link T} is available for consumption. The
     * {@code handler} will persist by default.
     *
     * @param handler Called when the instance has been retrieved and is ready
     *                for consumption.
     * @param checked If the receiver is shutdown and {@code checked} is
     *                {@code true} then {@link IllegalStateException} is
     *                thrown, otherwise the {@code handler} gets ignored.
     */
    public final void get(Handler<T> handler, boolean checked) {
        get(handler, checked, true);
    }

    /**
     * Guarantees the instance of {@link T} is available for consumption.
     *
     * @param handler Called when the instance has been retrieved and is ready
     *                for consumption.
     * @param checked If the receiver is shutdown and {@code checked} is
     *                {@code true} then {@link IllegalStateException} is
     *                thrown, otherwise the {@code handler} gets ignored.
     * @param persist Whether to persist the handler in a cache if the instance
     *                is not ready yet or an exception has occurred.
     */
    public final void get(final Handler<T> handler,
                          final boolean checked,
                          final boolean persist) {
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
                if (handler != null && persist) {
                    list.add(handler);
                }
                if (instantiationFut != null) {
                    return;
                }
                ScheduledThreadPoolExecutor stpe = getSTPE();
                InstantiationRunner runner = new InstantiationRunner();
                instantiationFut = stpe.scheduleWithFixedDelay(runner, 0, delay, timeUnit);
            }
        }
        T tmp;
        synchronized (this) {
            tmp = instance;
        }
        if (tmp != null && handler != null) {
            try {
                handler.handle(tmp);
            } catch (Exception e) {
                if (invalidateInstance(e)) {
                    synchronized (this) {
                        instance = null;
                    }
                    reattempt = true;
                } else {
                    LOGGER.error("Unhandled exception", e);
                }
            }
        } else if (tmp == null) {
            reattempt = true;
        }
        if (reattempt && persist) {
            get(handler, checked);
        }
    }

    /**
     * Shuts down the receiver from preventing any new instances to be created.
     * If the looping feature is enabled then it will be shutdown as well.
     *
     * @return The underlying instance of {@link T}, which can be
     * {@code null}, to allow freeing any resources the instance holds.
     */
    public T shutdown() {
        synchronized (this) {
            running = false;
        }

        if (loopFut != null) {
            try {
                loopFut.cancel(true);
            } catch (Exception ignored) {
            }
            loopFut = null;
        }

        T tmp;
        synchronized (this) {
            tmp = instance;
            list.clear();
            instance = null;
        }
        stopRunner();
        return tmp;
    }

    /**
     * Cancels the instantiation scheduled future from running any further.
     */
    private void stopRunner() {
        if (instantiationFut != null) {
            try {
                instantiationFut.cancel(true);
            } catch (Exception ignored) {
            }
            instantiationFut = null;
        }
    }

    private void initializeLoop() {
        ScheduledThreadPoolExecutor stpe = getSTPE();
        loopFut = stpe.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                get(new Handler<T>() {
                    @Override
                    public void handle(T event) {
                        onLoop(event);
                    }
                }, false, false);
            }
        }, 0, delay, timeUnit);
    }

    private class InstantiationRunner implements Runnable {
        @Override
        public void run() {
            synchronized (GuaranteedReceiver.this) {
                try {
                    instance = instantiate();
                    stopRunner();
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
            final Container<Boolean> doBreak = new Container<>();
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
                            }
                            if (inst == null) {
                                doBreak.setValue(true);
                                latch.countDown();
                                return;
                            }
                            latch.countDown();
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
                if (doBreak.getValue()) {
                    break;
                }
            }
        }
    }

    private static ScheduledThreadPoolExecutor getSTPE() {
        return Objects.getDaemonThreadPool();
    }

    static {
        LOGGER = LoggerFactory.getLogger(GuaranteedReceiver.class);
    }
}
