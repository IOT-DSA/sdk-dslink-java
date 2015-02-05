package org.dsa.iot.dslink.util;

import lombok.*;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.dsa.iot.core.ECKeyPair;
import org.vertx.java.core.json.JsonObject;

import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

/**
 * Client accepted from the server
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public class Client {

    @NonNull
    private final String dsId;

    private final ECKeyPair tempKey = generateTempKey();

    private final String salt = generateSalt();
    private String saltS = generateSalt();

    @Setter
    private boolean setup = false;

    private byte[] sharedSecret = null;

    public void parse(JsonObject obj) {

    }

    public void setSharedSecret(@NonNull byte[] bytes) {
        sharedSecret = bytes.clone();
    }

    public byte[] getSharedSecret() {
        return sharedSecret == null ? null : sharedSecret.clone();
    }

    private byte[] generateSecret() {
        SecureRandom rand = new SecureRandom();
        byte[] b = new byte[32];
        rand.nextBytes(b);
        return b;
    }

    private String generateSalt() {
        return new String(generateSecret(), Charset.forName("UTF-8"));
    }

    @SneakyThrows
    private ECKeyPair generateTempKey() {
        KeyPairGenerator gen = new KeyPairGeneratorSpi.ECDH();
        ECGenParameterSpec params = new ECGenParameterSpec("SECP256R1");
        gen.initialize(params);

        KeyPair key = gen.generateKeyPair();
        return new ECKeyPair((BCECPrivateKey) key.getPrivate(),
                                (BCECPublicKey) key.getPublic());
    }
}
