package org.dsa.iot.responder.connection.handshake;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.digest.SHA384;
import org.bouncycastle.util.encoders.Base64;
import org.vertx.java.core.json.JsonObject;

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

        SHA384.Digest sha = new SHA384.Digest();
        byte[] hash = sha.digest(pubKeyInfo.getEncoded());

        this.publicKey = Base64.toBase64String(pubParams.getModulus().toByteArray());
        this.dsId = dsIdPrefix + "-" + Base64.toBase64String(hash);

    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.putString("publicKey", publicKey);
        obj.putString("zone", zone);
        obj.putBoolean("isRequester", isRequester);
        obj.putBoolean("isResponder", isResponder);
        return obj;
    }

    @SuppressWarnings("ConstantConditions")
    public static HandshakeClient generate() {
        try {
            RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
            gen.init(new RSAKeyGenerationParameters(HandshakeClient.PUBLIC_EXPONENT,
                    new SecureRandom(),
                    HandshakeClient.KEY_STRENGTH,
                    HandshakeClient.KEY_CERTAINTY));
            AsymmetricCipherKeyPair key = gen.generateKeyPair();

            String zone = "default";

            boolean isRequester = false;
            boolean isResponder = true;
            return new HandshakeClient("dslink-test", key, zone, isRequester, isResponder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
