package org.dsa.iot.dslink.connection.connector;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.util.HttpClientUtils;
import org.dsa.iot.dslink.util.UrlBase64;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

import java.io.UnsupportedEncodingException;

/**
 * Handles connecting to web socket servers.
 * @author Samuel Grenier
 */
public class WebSocketConnector extends RemoteEndpoint {

    private Requester requester;

    private WebSocket webSocket;
    private Handler<JsonObject> jsonHandler;
    private Handler<NetworkClient> clientHandler;

    private boolean isBecomingActive = false;
    private boolean isActive = false;

    @Override
    public void activate() {
        isBecomingActive = true;
        HttpClient client = HttpClientUtils.configure(getEndpoint());
        RemoteHandshake handshake = getRemoteHandshake();

        String uri = handshake.getWsUri() + "?auth=";
        try {
            byte[] salt = handshake.getSalt().getBytes("UTF-8");
            byte[] sharedSecret = handshake.getRemoteKey().getSharedSecret();

            Buffer buffer = new Buffer(salt.length + sharedSecret.length);
            buffer.appendBytes(salt);
            buffer.appendBytes(sharedSecret);

            SHA256.Digest sha = new SHA256.Digest();
            byte[] digested = sha.digest(buffer.getBytes());
            String encoded = UrlBase64.encode(digested);
            uri += encoded + "&dsId=" + getLocalHandshake().getDsId();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        client.connectWebsocket(uri, new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket event) {
                isActive = true;
                isBecomingActive = false;
                WebSocketConnector.this.webSocket = event;

                Handler<NetworkClient> handler = clientHandler;
                if (handler != null) {
                    handler.handle(WebSocketConnector.this);
                }

                event.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        event.printStackTrace();
                        isActive = false;
                    }
                });

                event.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        Handler<JsonObject> handler = jsonHandler;
                        if (handler != null) {
                            String string = event.toString("UTF-8");
                            handler.handle(new JsonObject(string));
                        }
                    }
                });

                event.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        isActive = false;
                    }
                });
            }
        });
    }

    @Override
    public void deactivate() {
        if (webSocket != null) {
            webSocket.close();
        }
    }

    @Override
    public void write(JsonObject object) {
        if (!isActive()) {
            throw new IllegalStateException("Connector is not connected completely");
        } else if (object == null) {
            throw new NullPointerException("object");
        }
        webSocket.writeTextFrame(object.encode());
    }

    @Override
    public void setRequester(Requester requester) {
        this.requester = requester;
        requester.setRemoteEndpoint(this);
    }

    @Override
    public void setClientConnectedHandler(Handler<NetworkClient> handler) {
        this.clientHandler = handler;
    }

    @Override
    public void setDataHandler(Handler<JsonObject> handler) {
        this.jsonHandler = handler;
    }

    @Override
    public boolean isBecomingActive() {
        return isBecomingActive;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public Requester getRequester() {
        return requester;
    }
}
