package org.dsa.iot.dslink.handshake;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * @author Samuel Grenier
 */
public class RemoteKeyTest {

    /**
     * Tests shared secret validation.
     */
    @Test
    public void sharedSecretTest() {
        // Client gives its key to the server
        LocalKeys localKeys = LocalKeys.generate();

        // Server generates a temporary local key
        ECPoint point = localKeys.getPublicKey().getQ();
        LocalKeys tempLocalKey = LocalKeys.generate();
        point = point.multiply(tempLocalKey.getPrivateKey().getD());
        BigInteger bi = point.normalize().getXCoord().toBigInteger();
        byte[] sharedSecret = RemoteKey.normalize(bi.toByteArray());

        // Client receives temp key
        String tempKey = tempLocalKey.encodedPublicKey();
        RemoteKey remoteKey = RemoteKey.generate(localKeys, tempKey);
        Assert.assertArrayEquals(sharedSecret, remoteKey.getSharedSecret());
    }

    /**
     * Tests the normalization of the shared secret. This ensures a consistency
     * throughout the system.
     */
    @Test
    public void normalizationTest() {
        byte[] b = new byte[3];
        b = RemoteKey.normalize(b);
        Assert.assertEquals(32, b.length);

        b = new byte[40];
        b = RemoteKey.normalize(b);
        Assert.assertEquals(32, b.length);

        b = new byte[] {1, 2, 3};
        b = RemoteKey.normalize(b);
        Assert.assertEquals(1, b[29]);
        Assert.assertEquals(2, b[30]);
        Assert.assertEquals(3, b[31]);

        b = new byte[100];
        b[50] = 4;
        b[99] = 3;
        b[98] = 2;
        b[97] = 1;
        b = RemoteKey.normalize(b);
        Assert.assertEquals(1, b[29]);
        Assert.assertEquals(2, b[30]);
        Assert.assertEquals(3, b[31]);
        Assert.assertFalse(Arrays.contains(toIntArray(b), 4));
    }

    private int[] toIntArray(byte[] bytes) {
        int[] array = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            array[i] = bytes[i];
        }
        return array;
    }
}
