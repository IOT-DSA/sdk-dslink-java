package org.dsa.iot.dslink.handshake;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests key serialization, deserialization, and generation.
 * @author Samuel Grenier
 */
public class LocalKeysTest {

    /**
     * Ensures serialization and deserialization of the keys are consistently
     * operated on. Consistency in hashcode is tested as well.
     */
    @Test
    public void encodeDecodeConsistency() {
        LocalKeys keys = LocalKeys.generate();
        LocalKeys newKeys = LocalKeys.deserialize(keys.serialize());

        Assert.assertEquals(keys, newKeys);
        Assert.assertEquals(keys.hashCode(), newKeys.hashCode());
    }

}
