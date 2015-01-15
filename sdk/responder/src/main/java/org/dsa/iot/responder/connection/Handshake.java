package org.dsa.iot.responder.connection;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.digest.SHA384;
import org.bouncycastle.util.encoders.Base64;
import org.vertx.java.core.json.JsonObject;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author Samuel Grenier
 */
public class Handshake {

    public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);
    public static final int KEY_STRENGTH = 2048;
    public static final int KEY_CERTAINTY = 32;

    public final String dsId;
    public final String publicKey;
    public final String zone;
    public final boolean isRequester;
    public final boolean isResponder;

    private Handshake(String dsId, String publicKey, String zone,
                     boolean isRequester, boolean isResponder) {
        this.dsId = dsId;
        this.publicKey = publicKey;
        this.zone = zone;
        this.isRequester = isRequester;
        this.isResponder = isResponder;
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
    public static Handshake generate() {
        // TODO: ability for dslink implementer to provide a preset public key
        String publicKey;
        String dsId = "dslink-test-"; // TODO: ability to provide custom id prefix
        try {
            RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
            gen.init(new RSAKeyGenerationParameters(Handshake.PUBLIC_EXPONENT,
                    new SecureRandom(),
                    Handshake.KEY_STRENGTH,
                    Handshake.KEY_CERTAINTY));
            AsymmetricCipherKeyPair key = gen.generateKeyPair();

            RSAKeyParameters pubParams = (RSAKeyParameters) key.getPublic();
            publicKey = Base64.toBase64String(pubParams.getModulus().toByteArray());

            SubjectPublicKeyInfo info = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key.getPublic());
            SHA384.Digest sha = new SHA384.Digest();
            byte[] hash = sha.digest(info.getEncoded());
            dsId += Base64.toBase64String(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String zone = "default";

        boolean isRequester = false;
        boolean isResponder = true;
        return new Handshake(dsId, publicKey, zone, isRequester, isResponder);
    }
}
