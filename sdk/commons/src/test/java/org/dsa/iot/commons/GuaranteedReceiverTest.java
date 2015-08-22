package org.dsa.iot.commons;

import org.junit.Assert;
import org.junit.Test;
import org.vertx.java.core.Handler;

import java.util.concurrent.CountDownLatch;

/**
 * @author Samuel Grenier
 */
public class GuaranteedReceiverTest {

    @Test(expected = IllegalStateException.class)
    public void shutdownReceiverTest() throws InterruptedException {
        GuaranteedReceiver<Object> gr = new GuaranteedReceiver<Object>(1) {
            @Override
            protected Object instantiate() throws Exception {
                return new Object();
            }

            @Override
            protected boolean invalidateInstance(Exception e) {
                return false;
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        gr.get(new Handler<Object>() {
            @Override
            public void handle(Object event) {
                Assert.assertNotNull(event);
                latch.countDown();
            }
        }, true);
        latch.await();
        gr.shutdown();
        gr.get(null, true);
    }

}
