package org.dsa.iot.core;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class SyncTest {

    @Test
    public void sync() {
        SyncHandler<Integer> handler = new SyncHandler<>();
        handler.handle(1);
        Assert.assertNotNull(handler.get());
    }
}
