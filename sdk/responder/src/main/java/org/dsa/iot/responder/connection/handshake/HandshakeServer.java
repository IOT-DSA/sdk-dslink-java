package org.dsa.iot.responder.connection.handshake;

import org.bouncycastle.crypto.engines.RSAEngine;
import org.dsa.iot.core.SyncHandler;
import org.dsa.iot.core.URLInfo;
import org.dsa.iot.core.Utils;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.util.concurrent.TimeUnit;

/**
 * Holds handshake information about a server.
 * @author Samuel Grenier
 */
public class HandshakeServer {

    public final String dsId;
    public final String publicKey;
    public final String wsUri;
    public final String httpUri;
    public final byte[] nonce;
    public final String salt;
    public final String saltS;
    public final Integer updateInterval;

    public HandshakeServer(String dsId, String publicKey, String wsUri,
                           String httpUri, byte[] nonce, String salt,
                           String saltS, Integer updateInterval) {
        this.dsId = dsId;
        this.publicKey = publicKey;
        this.wsUri = wsUri;
        this.httpUri = httpUri;
        this.nonce = nonce;
        this.salt = salt;
        this.saltS = saltS;
        this.updateInterval = updateInterval;
    }

    public static HandshakeServer perform(String url, HandshakeClient hc) {
        return perform(URLInfo.parse(url), hc);
    }

    public static HandshakeServer perform(URLInfo url, HandshakeClient hc) {
        return perform(url, hc, true);
    }

    public static HandshakeServer perform(URLInfo url,
                                          HandshakeClient hc,
                                          boolean verifySsl) {
        HttpClient client = Utils.VERTX.createHttpClient();
        client.setHost(url.host).setPort(url.port);
        if (url.secure) {
            client.setSSL(true);
            client.setVerifyHost(verifySsl);
        }

        SyncHandler<HttpClientResponse> reqHandler = new SyncHandler<>();
        HttpClientRequest req = client.post(url.path + "?dsId=" + hc.dsId, reqHandler);

        String encoded = hc.toJson().encode();
        req.putHeader("Content-Length", String.valueOf(encoded.length()));
        req.write(encoded);
        req.end();

        HttpClientResponse resp = reqHandler.get(2, TimeUnit.SECONDS);
        if (resp == null)
            throw new NullPointerException("resp");
        SyncHandler<Buffer> bufHandler = new SyncHandler<>();
        resp.bodyHandler(bufHandler);

        Buffer buf = bufHandler.get();
        JsonObject obj = new JsonObject(buf.toString());

        String dsId = obj.getString("dsId");
        String publicKey = obj.getString("publicKey");
        String wsUri = obj.getString("wsUri");
        String httpUri = obj.getString("httpUri");
        byte[] nonce = decryptNonce(hc, obj.getString("encryptedNonce"));
        String salt = obj.getString("salt");
        String saltS = obj.getString("saltS");
        Integer updateInterval = obj.getInteger("updateInterval");

        return new HandshakeServer(dsId, publicKey, wsUri, httpUri,
                                    nonce, salt, saltS, updateInterval);
    }

    private static byte[] decryptNonce(HandshakeClient client,
                                       String encryptedNonce) {
        byte[] encrypted = Base64.decode(encryptedNonce);
        RSAEngine engine = new RSAEngine();
        engine.init(false, client.privKeyInfo);
        return engine.processBlock(encrypted, 0, encrypted.length);
    }
}
