package org.dsa.iot.dslink.connection.connector;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.util.HttpClientUtils;
import org.dsa.iot.dslink.util.UrlBase64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.UnsupportedEncodingException;

/**
 * Handles connecting to web socket servers.
 *
 * @author Samuel Grenier
 */
public class WebSocketConnector extends RemoteEndpoint {

    private static final Logger LOGGER;

    private WebSocket webSocket;
    private Handler<JsonArray> requestHandler;
    private Handler<JsonArray> responseHandler;
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
                        Handler<JsonArray> reqHandler = requestHandler;
                        Handler<JsonArray> respHandler = responseHandler;

                        String string = event.toString("UTF-8");
                        JsonObject obj = new JsonObject(string);
                        print(true, string);

                        JsonArray requests = obj.getArray("requests");
                        if (!(reqHandler == null || requests == null)) {
                            reqHandler.handle(requests);
                        }

                        JsonArray responses = obj.getArray("responses");
                        if (!(respHandler == null || responses == null)) {
                            respHandler.handle(responses);
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
        String out = object.encode();
        print(false, out);
        webSocket.writeTextFrame(out);
    }

    @Override
    public void setClientConnectedHandler(Handler<NetworkClient> handler) {
        this.clientHandler = handler;
    }

    @Override
    public void setRequestDataHandler(Handler<JsonArray> handler) {
        if (!isResponder()) {
            throw new RuntimeException("This client is not a responder");
        }
        this.requestHandler = handler;
    }

    @Override
    public void setResponseDataHandler(Handler<JsonArray> handler) {
        if (!isRequester()) {
            throw new RuntimeException("This client is not a requester");
        }
        this.responseHandler = handler;
    }

    @Override
    public boolean isRequester() {
        return getLocalHandshake().isRequester();
    }

    @Override
    public boolean isResponder() {
        return getLocalHandshake().isResponder();
    }

    @Override
    public boolean isBecomingActive() {
        return isBecomingActive;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    private void print(boolean inOrOut, String data) {
        if (LOGGER.isDebugEnabled()) {
            String string = getRemoteHandshake().getDsId();
            if (string == null) {
                string = "Remote endpoint ";
            }
            string += inOrOut ? "--> " : "<-- ";
            string += data;
            LOGGER.debug(string);
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(WebSocketConnector.class);
    }
}
