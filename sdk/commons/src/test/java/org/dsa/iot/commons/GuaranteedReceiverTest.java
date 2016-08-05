package org.dsa.iot.commons;

import org.dsa.iot.dslink.util.handler.Handler;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author Samuel Grenier
 */
public class GuaranteedReceiverTest {

    @Test(expected = IllegalStateException.class)
    public void receiverShutdownThrowsStateException() throws InterruptedException {
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
