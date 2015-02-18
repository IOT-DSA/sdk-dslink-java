package org.dsa.iot.dslink.connection.connector.server;

import lombok.*;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.dsa.iot.core.ECKeyPair;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.requester.RequestTracker;
import org.dsa.iot.dslink.responder.ResponseTracker;
import org.vertx.java.core.http.WebSocketBase;
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
public class ServerClient implements Client {

    @NonNull private final String dsId;
    @NonNull private final RequestTracker requestTracker;
    @NonNull private final ResponseTracker responseTracker;
    
    @Setter private WebSocketBase webSocket = null;
    @Setter private boolean connected = false;
    @Setter private boolean setup = false;
    
    private final ECKeyPair tempKey = generateTempKey();
    private final String salt = generateSalt();
    private String saltS = generateSalt();
    private byte[] sharedSecret = null;

    @Override
    public boolean write(JsonObject obj) {
        if (connected) {
            webSocket.writeTextFrame(obj.encode());
            return true;
        }
        return false;
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
