package org.dsa.iot.commons;

import org.junit.Assert;
import org.junit.Test;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class GuaranteedReceiverTest {

    @Test(expected = IllegalStateException.class)
    public void checkedTest() {
        GuaranteedReceiver<Object> gr = new GuaranteedReceiver<Object>(5) {
            @Override
            protected Object instantiate() throws Exception {
                return new Object();
            }

            @Override
            protected boolean invalidateInstance(Exception e) {
                return false;
            }
        };
        gr.get(new Handler<Object>() {
            @Override
            public void handle(Object event) {
                Assert.assertNotNull(event);
            }
        }, true);
        gr.shutdown();
        gr.get(null, true);
    }

}
