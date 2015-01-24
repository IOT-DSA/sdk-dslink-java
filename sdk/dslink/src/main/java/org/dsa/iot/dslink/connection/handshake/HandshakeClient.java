package org.dsa.iot.dslink.connection.handshake;

import lombok.Getter;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Handshake information for the client.
 * @author Samuel Grenier
 */
@Getter
public class HandshakeClient {

    public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);
    public static final int KEY_STRENGTH = 2048;
    public static final int KEY_CERTAINTY = 32;

    private final RSAPrivateCrtKeyParameters privKeyInfo;
    private final RSAKeyParameters pubKeyInfo;

    private final String dsId;
    private final String publicKey;
    private final String zone;
    private final boolean isRequester;
    private final boolean isResponder;

    private HandshakeClient(String dsIdPrefix,
                            AsymmetricCipherKeyPair key, String zone,
                            boolean isRequester, boolean isResponder)
                                                    throws IOException {
        this.zone = zone;
        this.isRequester = isRequester;
        this.isResponder = isResponder;

        this.pubKeyInfo = (RSAKeyParameters) key.getPublic();
        this.privKeyInfo = (RSAPrivateCrtKeyParameters) key.getPrivate();

        BigInteger modulus = pubKeyInfo.getModulus();
        byte[] modBytes = modulus.toByteArray();
        modBytes = Arrays.copyOfRange(modBytes, 1, modBytes.length);

        this.publicKey = Base64.encodeBytes(modBytes, Base64.URL_SAFE);

        SHA256.Digest sha = new SHA256.Digest();
        byte[] hash = sha.digest(modBytes);

        String encoded = Base64.encodeBytes(hash, Base64.URL_SAFE);
        this.dsId = dsIdPrefix + "-" + encoded.substring(0, encoded.length() - 1);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.putString("publicKey", publicKey);
        obj.putString("zone", zone);
        obj.putBoolean("isRequester", isRequester);
        obj.putBoolean("isResponder", isResponder);
        return obj;
    }

    /**
     * The zone used will be the default zone.
     * @param dsId ID prefix of the client appended by a dash and a hash
     * @return The generated client
     */
    public static HandshakeClient generate(String dsId) {
        return generate(dsId, "default");
    }

    /**
     * @param dsId ID prefix of the client appended by a dash and a hash
     * @return The generated client
     */
    @SuppressWarnings("ConstantConditions")
    public static HandshakeClient generate(String dsId, String zone) {
        if (dsId == null || dsId.isEmpty())
            throw new IllegalArgumentException("dsId");
        else if (zone == null || zone.isEmpty())
            throw new IllegalArgumentException("zone");
        try {
            RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
            gen.init(new RSAKeyGenerationParameters(PUBLIC_EXPONENT,
                    new SecureRandom(),
                    KEY_STRENGTH,
                    KEY_CERTAINTY));
            AsymmetricCipherKeyPair key = gen.generateKeyPair();

            boolean isRequester = false;
            boolean isResponder = true;
            return new HandshakeClient(dsId, key, zone, isRequester, isResponder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
