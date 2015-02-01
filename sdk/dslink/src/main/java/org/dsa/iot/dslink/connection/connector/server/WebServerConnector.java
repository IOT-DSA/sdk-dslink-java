package org.dsa.iot.dslink.connection.connector.server;

import lombok.SneakyThrows;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles web sockets and HTTP connections.
 * @author Samuel Grenier
 */
public class WebServerConnector extends ServerConnector {

    private final Map<String, Client> clients = new HashMap<>();

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
                    if (!validID(clientDsId, pubKey) || (getClient(clientDsId) != null)) {
                        resp.setStatusCode(401); // Unauthorized
                    } else {
                        Client c = new Client(clientDsId);
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
                        addClient(c);
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
            @SneakyThrows
            public void handle(ServerWebSocket event) {
                if (event.path().equals("/ws")) {
                    final String dsId = event.headers().get("dsId");
                    final String auth = event.headers().get("auth");

                    event.closeHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            removeClient(dsId);
                        }
                    });

                    final Client client = getClient(dsId);
                    if (client == null || client.isSetup()) {
                        event.reject();
                    } else {
                        byte[] originalHash = UrlBase64.decode(Utils.addPadding(auth, true));

                        Buffer buffer = new Buffer(client.getSalt().length() + client.getDecryptedNonce().length);
                        buffer.appendBytes(client.getSalt().getBytes("UTF-8"));
                        buffer.appendBytes(client.getDecryptedNonce());

                        SHA256.Digest digest = new SHA256.Digest();
                        byte[] output = digest.digest(buffer.getBytes());

                        if (!Arrays.equals(originalHash, output)) {
                            event.reject();
                        } else {
                            client.setSetup(true);
                            event.dataHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer event) {
                                    client.parse(new JsonObject(event.toString("UTF-8")));
                                }
                            });
                        }
                    }
                } else {
                    event.reject();
                }
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

    private synchronized void addClient(Client c) {
        clients.put(c.getDsId(), c);
    }

    private synchronized void removeClient(String name) {
        clients.remove(name);
    }

    private synchronized Client getClient(String name) {
        return clients.get(name);
    }
}
