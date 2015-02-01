package org.dsa.iot.dslink.connection.connector.server;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.util.encoders.UrlBase64;
import org.dsa.iot.core.SyncHandler;
import org.dsa.iot.core.Utils;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.util.Client;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Handles web sockets and HTTP connections.
 * @author Samuel Grenier
 */
public class WebServerConnector extends ServerConnector {

    public WebServerConnector(HandshakeClient client) {
        super(client);
    }

    @Override
    public void start(int port, String bindAddr) {
        HttpServer server = Utils.VERTX.createHttpServer();
        server.requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                HttpServerResponse resp = event.response();
                resp.setStatusCode(200);
                if (!event.method().equals("POST")) {
                    resp.setStatusCode(405); // Method not allowed
                } else if (event.path().equals("/conn")) {
                    SyncHandler<Buffer> buf = new SyncHandler<>();
                    event.bodyHandler(buf);
                    JsonObject clientJson = new JsonObject(buf.get().toString("UTF-8"));

                    String clientDsId = clientJson.getString("dsId");
                    String pubKey = clientJson.getString("publicKey");
                    if (!validID(clientDsId, pubKey)) {
                        resp.setStatusCode(401); // Unauthorized
                    } else {
                        Client c = new Client(clientDsId, generateNonce(),
                                generateSalt(), generateSalt());
                        JsonObject obj = new JsonObject();
                        obj.putString("dsId", getClient().getDsId());
                        obj.putString("publicKey", getClient().getPublicKey());
                        obj.putString("wsUri", "/ws");
                        obj.putString("httpUri", "/http");
                        {
                            BigInteger nonce = new BigInteger(1, c.getDecryptedNonce());
                            nonce = nonce.modPow(HandshakeClient.PUBLIC_EXPONENT,
                                    getClient().getPubKeyInfo().getModulus());
                            byte[] encoded = UrlBase64.encode(nonce.toByteArray());
                            obj.putString("encryptedNonce", new String(encoded));

                        }
                        obj.putString("salt", c.getSalt());
                        obj.putString("saltS", c.getSaltS());
                        // TODO: updateInterval
                        resp.write(obj.encode(), "UTF-8");
                    }

                } if (event.path().equals("/http")) {
                    resp.setStatusCode(501); // Not implemented
                } else {
                    // TODO: handle ws and http endpoints
                    resp.setStatusCode(404); // Page not found
                }
                resp.end();
            }
        });

        server.websocketHandler(new Handler<ServerWebSocket>() {
            @Override
            public void handle(ServerWebSocket event) {

            }
        });

        if (bindAddr != null)
            server.listen(port, bindAddr);
        else
            server.listen(port);
    }

    @Override
    public void stop() {

    }

    private boolean validID(String dsId, String publicKey) {
        String encoded = dsId.substring(dsId.length() - 43);
        byte[] originalHash = UrlBase64.decode(encoded);

        SHA256.Digest sha = new SHA256.Digest();
        byte[] pubKey = UrlBase64.decode(publicKey);
        byte[] newHash = sha.digest(pubKey);
        return Arrays.equals(originalHash, newHash);
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
