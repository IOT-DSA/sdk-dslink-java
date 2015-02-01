package org.dsa.iot.dslink.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.vertx.java.core.json.JsonObject;

import java.security.SecureRandom;

/**
 * Client accepted from the server
 * @author Samuel Grenier
 */
@Getter
@RequiredArgsConstructor
public class Client {

    private final String dsId;
    private final byte[] decryptedNonce = generateNonce();

    private final String salt = generateSalt();
    private String saltS = generateSalt();

    @Setter
    private boolean setup = false;

    public void parse(JsonObject obj) {

    }

    private byte[] generateNonce() {
        SecureRandom rand = new SecureRandom();
        byte[] b = new byte[16];
        rand.nextBytes(b);
        return b;
    }

    private String generateSalt() {
        return new String(generateNonce());
    }
}
