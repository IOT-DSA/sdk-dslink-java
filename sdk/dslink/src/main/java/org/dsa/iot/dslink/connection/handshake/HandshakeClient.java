package org.dsa.iot.dslink.connection.handshake;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.UrlBase64;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

/**
 * Handshake information for the client.
 * @author Samuel Grenier
 */
@Getter
public class HandshakeClient {

    private final BCECPrivateKey privKeyInfo;
    private final BCECPublicKey pubKeyInfo;

    private final String dsId;
    private final String publicKey;
    private final String zone;
    private final boolean isRequester;
    private final boolean isResponder;

    private HandshakeClient(String dsIdPrefix,
                            KeyPair key, String zone,
                            boolean isRequester, boolean isResponder)
                                                    throws IOException {
        this.zone = zone;
        this.isRequester = isRequester;
        this.isResponder = isResponder;

        this.pubKeyInfo = (BCECPublicKey) key.getPublic();
        this.privKeyInfo = (BCECPrivateKey) key.getPrivate();

        byte[] pubKey = pubKeyInfo.getQ().getEncoded(false);
        val pKey = new String(UrlBase64.encode(pubKey), "UTF-8");
        this.publicKey = pKey.substring(0, pKey.length() - 1);

        SHA256.Digest sha = new SHA256.Digest();
        byte[] hash = sha.digest(pubKey);

        String encoded = new String(UrlBase64.encode(hash), "UTF-8");
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

    public static HandshakeClient generate(@NonNull String dsId,
                                            boolean isRequester,
                                            boolean isResponder) {
        return generate(dsId, "default", isRequester, isResponder);
    }

    /**
     * @param dsId ID prefix of the client appended by a dash and a hash
     * @param zone Zone to run the client in
     * @param isRequester Whether or not this is a requester
     * @param isResponder Whether or not this is a responder
     * @return The generated client
     */
    public static HandshakeClient generate(@NonNull String dsId,
                                           @NonNull String zone,
                                           boolean isRequester,
                                           boolean isResponder) {
        if (dsId.isEmpty())
            throw new IllegalArgumentException("dsId");
        else if (zone.isEmpty())
            throw new IllegalArgumentException("zone");
        try {
            KeyPairGenerator gen = new KeyPairGeneratorSpi.ECDH();
            ECGenParameterSpec params = new ECGenParameterSpec("SECP256R1");
            gen.initialize(params);

            KeyPair key = gen.generateKeyPair();
            return new HandshakeClient(dsId, key, zone, isRequester, isResponder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
}
