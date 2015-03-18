package org.dsa.iot.dslink.handshake;

import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.dsa.iot.dslink.util.UrlBase64;

import java.math.BigInteger;

/**
 * Handles remote keys. The shared secret will be decrypted here.
 *
 * @author Samuel Grenier
 */
public class RemoteKey {

    private final byte[] sharedSecret;

    /**
     * Populates the remote key with encryption data from the server.
     *
     * @param sharedSecret Shared secret retrieved from the server
     */
    private RemoteKey(byte[] sharedSecret) {
        this.sharedSecret = sharedSecret.clone();
    }

    /**
     * Retrieves the decrypted shared secret.
     *
     * @return shared secret
     */
    public byte[] getSharedSecret() {
        return sharedSecret.clone();
    }

    /**
     * @param keys    Local keys necessary for the decryption
     * @param tempKey Temporary key received from the server
     * @return A remote key.
     */
    public static RemoteKey generate(LocalKeys keys, String tempKey) {
        byte[] decoded = UrlBase64.decode(tempKey);
        ECParameterSpec params = keys.getPrivateKey().getParameters();
        ECPoint point = params.getCurve().decodePoint(decoded);
        ECPublicKeySpec spec = new ECPublicKeySpec(point, params);
        point = spec.getQ().multiply(keys.getPrivateKey().getD());
        BigInteger bi = point.normalize().getXCoord().toBigInteger();
        byte[] sharedSecret = bi.toByteArray();
        sharedSecret = normalize(sharedSecret);
        return new RemoteKey(sharedSecret);
    }

    /**
     * Ensures the shared secret is always 32 bytes in length.
     *
     * @param sharedSecret Shared secret to normalize
     * @return Normalized shared secret
     */
    public static byte[] normalize(byte[] sharedSecret) {
        if (sharedSecret.length < 32) {
            byte[] fixed = new byte[32];
            int len = sharedSecret.length;
            System.arraycopy(sharedSecret, 0, fixed, 32 - len, len);
            sharedSecret = fixed;
        } else if (sharedSecret.length > 32) {
            byte[] fixed = new byte[32];
            System.arraycopy(sharedSecret, sharedSecret.length - 32, fixed, 0, fixed.length);
            sharedSecret = fixed;
        }
        return sharedSecret;
    }
}
