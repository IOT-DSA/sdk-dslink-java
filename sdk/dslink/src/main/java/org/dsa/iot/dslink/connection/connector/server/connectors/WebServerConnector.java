package org.dsa.iot.dslink.connection.connector.server.connectors;

import lombok.*;
import net.engio.mbassy.bus.MBassador;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.UrlBase64;
import org.dsa.iot.core.Utils;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.connection.connector.server.ServerClient;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.connection.handshake.HandshakeServer;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.dsa.iot.dslink.events.ClientConnectedEvent;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.events.IncomingDataEvent;
import org.dsa.iot.dslink.requester.RequestTracker;
import org.dsa.iot.dslink.responder.ResponseTracker;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Handles web sockets and HTTP connections.
 * @author Samuel Grenier
 */
public class WebServerConnector extends ServerConnector {

    private final Map<String, ServerClient> clients = new HashMap<>();
    private final HttpServer server = Utils.VERTX.createHttpServer();

    public WebServerConnector(MBassador<Event> bus, HandshakeClient client) {
        super(bus, client);
    }

    @Override
    @SneakyThrows
    public void start(int port, String bindAddr) {
        server.requestHandler(new HttpHandler());
        server.websocketHandler(new WebSocketHandler());

        CountDownLatch latch = new CountDownLatch(1);
        val handler = new AsyncHandler(latch);
        if (bindAddr != null)
            server.listen(port, bindAddr, handler);
        else
            server.listen(port, handler);
        latch.await();
        if (handler.getEvent().succeeded()) {
            setListening(true);
        }
    }

    @Override
    public void stop() {
        server.close();
        setListening(false);
    }

    private boolean validID(String dsId) {
        return dsId != null && dsId.length() >= 43;
    }

    private synchronized void addClient(ServerClient c) {
        clients.put(c.getDsId(), c);
    }

    private synchronized void removeClient(String name) {
        clients.remove(name);
    }

    private synchronized ServerClient getClient(String name) {
        return clients.get(name);
    }

    @RequiredArgsConstructor
    private class AsyncHandler implements Handler<AsyncResult<HttpServer>> {

        @NonNull
        private final CountDownLatch latch;

        @Getter
        private AsyncResult<HttpServer> event;

        @Override
        public void handle(AsyncResult<HttpServer> event) {
            this.event = event;
            if (event.failed()) {
                getBus().publish(new AsyncExceptionEvent(event.cause()));
            }
            latch.countDown();
        }
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
                @SneakyThrows
                public void handle(Buffer buf) {
                    JsonObject clientJson = new JsonObject(buf.toString("UTF-8"));

                    String clientDsId = req.params().get("dsId");
                    ServerClient client = getClient(clientDsId);
                    if (!validID(clientDsId) || (client != null && client.isSetup())) {
                        removeClient(clientDsId);
                        resp.setStatusCode(401); // Unauthorized
                        resp.end();
                    } else {
                        ServerClient c = new ServerClient(clientDsId,
                                                            new RequestTracker(),
                                                            new ResponseTracker());
                        JsonObject obj = new JsonObject();
                        obj.putString("dsId", getClient().getDsId());
                        obj.putString("publicKey", getClient().getPublicKey());
                        obj.putString("wsUri", "/ws");
                        obj.putString("httpUri", "/http");
                        {
                            String pubKey = clientJson.getString("publicKey");
                            pubKey = Utils.addPadding(pubKey, true);
                            byte[] decoded = UrlBase64.decode(pubKey);

                            ECParameterSpec params = getClient().getPrivKeyInfo().getParameters();
                            ECPoint point = params.getCurve().decodePoint(decoded);
                            ECPublicKeySpec spec = new ECPublicKeySpec(point, params);
                            point = spec.getQ().multiply(getClient().getPrivKeyInfo().getD());
                            byte[] sharedSecret = point.normalize().getXCoord().toBigInteger().toByteArray();
                            sharedSecret = HandshakeServer.normalize(sharedSecret);
                            c.setSharedSecret(sharedSecret);

                            byte[] encoded = getClient().getPubKeyInfo().getQ().getEncoded(false);
                            String key = new String(UrlBase64.encode(encoded), "UTF-8");
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
                final ServerClient client = getClient(dsId);

                event.closeHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        if (client != null) {
                            client.setConnected(false);
                        }
                        removeClient(dsId);
                    }
                });
                
                if (client == null || client.isSetup()) {
                    event.reject();
                    removeClient(dsId);
                } else {
                    client.setSetup(true);
                    client.setConnected(true);
                    client.setWebSocket(event);
                    byte[] originalHash = UrlBase64.decode(Utils.addPadding(auth, true));

                    Buffer buffer = new Buffer(client.getSalt().length() + client.getSharedSecret().length);
                    buffer.appendBytes(client.getSalt().getBytes("UTF-8"));
                    buffer.appendBytes(client.getSharedSecret());

                    SHA256.Digest digest = new SHA256.Digest();
                    byte[] output = digest.digest(buffer.getBytes());

                    if (!MessageDigest.isEqual(originalHash, output)) {
                        event.reject();
                        removeClient(dsId);
                    } else {
                        event.dataHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer event) {
                                val data = new JsonObject(event.toString("UTF-8"));
                                getBus().publish(new IncomingDataEvent(client, data));
                            }
                        });
                        getBus().publish(new ClientConnectedEvent(client));
                    }
                }
            } else {
                event.reject();
            }
        }
    }
}
