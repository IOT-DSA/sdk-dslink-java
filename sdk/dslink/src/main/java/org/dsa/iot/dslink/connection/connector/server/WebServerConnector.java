package org.dsa.iot.dslink.connection.connector.server;

import lombok.SneakyThrows;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.UrlBase64;
import org.dsa.iot.core.Utils;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.util.Client;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles web sockets and HTTP connections.
 * @author Samuel Grenier
 */
public class WebServerConnector extends ServerConnector {

    private final Map<String, Client> clients = new HashMap<>();
    private final HttpServer server = Utils.VERTX.createHttpServer();

    public WebServerConnector(HandshakeClient client) {
        super(client);
    }

    @Override
    public void start(int port, String bindAddr) {
        server.requestHandler(new HttpHandler());
        server.websocketHandler(new WebSocketHandler());

        if (bindAddr != null)
            server.listen(port, bindAddr);
        else
            server.listen(port);
        setListening(true);
    }

    @Override
    public void stop() {
        server.close();
        setListening(false);
    }

    private boolean validID(String dsId) {
        return dsId != null && dsId.length() >= 43;
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

    private class HttpHandler implements Handler<HttpServerRequest> {

        @Override
        public void handle(HttpServerRequest event) {
            final HttpServerResponse resp = event.response();
            if (!event.method().equals("POST")) {
                resp.setStatusCode(405);
                resp.end("Method not allowed");
            } else if (event.path().equals("/conn")) {
                handleConn(event, resp);
            } else if (event.path().equals("/http")) {
                handleHTTP(resp);
            } else {
                // TODO: http endpoints
                resp.setStatusCode(404); // Page not found
                resp.end();
            }
        }

        private void handleConn(final HttpServerRequest req,
                                final HttpServerResponse resp) {
            resp.setStatusCode(200);
            req.bodyHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buf) {
                    JsonObject clientJson = new JsonObject(buf.toString("UTF-8"));

                    String clientDsId = req.params().get("dsId");
                    Client client = getClient(clientDsId);
                    if (!validID(clientDsId) || (client != null && client.isSetup())) {
                        removeClient(clientDsId);
                        resp.setStatusCode(401); // Unauthorized
                        resp.end();
                    } else {
                        Client c = new Client(clientDsId);
                        JsonObject obj = new JsonObject();
                        obj.putString("dsId", getClient().getDsId());
                        obj.putString("publicKey", getClient().getPublicKey());
                        obj.putString("wsUri", "/ws");
                        obj.putString("httpUri", "/http");
                        {
                            String pubKey = clientJson.getString("publicKey");
                            pubKey = Utils.addPadding(pubKey, true);
                            byte[] decoded = UrlBase64.decode(pubKey);

                            ECParameterSpec params = getClient().getPubKeyInfo().getParameters();
                            ECPoint point = params.getCurve().decodePoint(decoded);
                            ECPublicKeySpec spec = new ECPublicKeySpec(point, params);
                            point = spec.getQ().multiply(getClient().getPrivKeyInfo().getD());
                            c.setSharedSecret(point.normalize().getXCoord().toBigInteger().toByteArray());

                            byte[] encoded = getClient().getPubKeyInfo().getQ().getEncoded(false);
                            String key = Base64.encodeBytes(encoded, Base64.URL_SAFE);
                            obj.putString("tempKey", key);
                        }
                        obj.putString("salt", c.getSalt());
                        obj.putString("saltS", c.getSaltS());
                        obj.putNumber("updateInterval", 200);
                        resp.end(obj.encode(), "UTF-8");
                        addClient(c);
                    }
                }
            });
        }

        private void handleHTTP(HttpServerResponse resp) {
            resp.setStatusCode(501);
            resp.end("Not yet implemented");
        }
    }

    private class WebSocketHandler implements Handler<ServerWebSocket> {

        @Override
        @SneakyThrows
        public void handle(ServerWebSocket event) {
            MultiMap params = Utils.parseQueryParams(event.uri());
            if (event.path().equals("/ws") && params != null) {
                final String dsId = params.get("dsId");
                final String auth = params.get("auth");

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

                    Buffer buffer = new Buffer(client.getSalt().length() + client.getSharedSecret().length);
                    buffer.appendBytes(client.getSalt().getBytes("UTF-8"));
                    buffer.appendBytes(client.getSharedSecret());

                    SHA256.Digest digest = new SHA256.Digest();
                    byte[] output = digest.digest(buffer.getBytes());

                    if (!MessageDigest.isEqual(originalHash, output)) {
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
    }
}
