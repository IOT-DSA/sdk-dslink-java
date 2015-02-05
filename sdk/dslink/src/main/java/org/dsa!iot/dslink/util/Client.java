package org.dsa.iot.dslink.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.dsa.iot.core.ECKeyPair;
import org.vertx.java.core.json.JsonObject;

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

    private final String dsId;
    private final ECKeyPair tempKey = generateTempKey();

    private final String salt = generateSalt();
    private String saltS = generateSalt();

    @Setter
    private boolean setup = false;

    @Setter
    private byte[] sharedSecret = null;

    public void parse(JsonObject obj) {

    }

    private byte[] generateSecret() {
        SecureRandom rand = new SecureRandom();
        byte[] b = new byte[32];
        rand.nextBytes(b);
        return b;
    }

    private String generateSalt() {
        return new String(generateSecret());
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
