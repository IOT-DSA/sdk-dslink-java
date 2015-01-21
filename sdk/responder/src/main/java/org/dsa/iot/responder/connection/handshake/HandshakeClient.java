package org.dsa.iot.responder.connection.handshake;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.digest.SHA384;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Handshake information for the client.
 * @author Samuel Grenier
 */
public class HandshakeClient {

    public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);
    public static final int KEY_STRENGTH = 2048;
    public static final int KEY_CERTAINTY = 32;

    public final CipherParameters privKeyInfo;
    public final SubjectPublicKeyInfo pubKeyInfo;

    public final String dsId;
    public final String publicKey;
    public final String zone;
    public final boolean isRequester;
    public final boolean isResponder;

    private HandshakeClient(String dsIdPrefix,
                            AsymmetricCipherKeyPair key, String zone,
                            boolean isRequester, boolean isResponder)
                                                    throws IOException {
        this.zone = zone;
        this.isRequester = isRequester;
        this.isResponder = isResponder;

        RSAKeyParameters pubParams = (RSAKeyParameters) key.getPublic();
        this.pubKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pubParams);
        this.privKeyInfo = key.getPrivate();

        BigInteger modulus = pubParams.getModulus();
        byte[] modBytes = modulus.toByteArray();
        this.publicKey = Base64.encodeBytes(modBytes, Base64.URL_SAFE);

        SHA384.Digest sha = new SHA384.Digest();
        byte[] hash = sha.digest(modBytes);
        this.dsId = dsIdPrefix + "-" + Base64.encodeBytes(hash, Base64.URL_SAFE);

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
            gen.init(new RSAKeyGenerationParameters(HandshakeClient.PUBLIC_EXPONENT,
                    new SecureRandom(),
                    HandshakeClient.KEY_STRENGTH,
                    HandshakeClient.KEY_CERTAINTY));
            AsymmetricCipherKeyPair key = gen.generateKeyPair();

            boolean isRequester = false;
            boolean isResponder = true;
            return new HandshakeClient(dsId, key, zone, isRequester, isResponder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
