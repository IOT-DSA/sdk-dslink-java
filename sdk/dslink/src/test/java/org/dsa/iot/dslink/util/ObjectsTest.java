package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.*;

/**
 * @author Samuel Grenier
 */
public class ObjectsTest {

    @Test
    public void nonNullObjects() {
        Objects.setDaemonThreadPool(null);
        Objects.setThreadPool(null);

        Assert.assertNotNull(Objects.getDaemonThreadPool());
        Assert.assertNotNull(Objects.getThreadPool());
        Assert.assertNotNull(Objects.getVertx());
    }

    @Test
    public void execution() {
        final CountDownLatch latch = new CountDownLatch(1);
        Objects.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                Assert.fail("Execution of the Runnable failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void afterExecuteException() {
        final CountDownLatch latch = new CountDownLatch(2);

        Objects.getDaemonThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        latch.countDown();
                    }
                });

                Assert.assertTrue(Thread.currentThread().isDaemon());
                throw new RuntimeException();
            }
        });

        Objects.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        latch.countDown();
                    }
                });

                Assert.assertFalse(Thread.currentThread().isDaemon());
                throw new RuntimeException();
            }
        });

        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                Assert.fail("Exception(s) failed to propagate");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void ignoredCancellation() {
        ScheduledFuture<?> fut = Objects.getThreadPool().schedule(new Runnable() {
            @Override
            public void run() {
            }
        }, 10, TimeUnit.SECONDS);
        fut.cancel(true);
    }
}
