package org.dsa.iot.responder.connection.connector;

import org.dsa.iot.core.URLInfo;
import org.dsa.iot.core.Utils;
import org.dsa.iot.responder.connection.Connector;
import org.dsa.iot.responder.connection.handshake.HandshakeServer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class WebSocketConnector extends Connector {

    protected HttpClient client;
    protected WebSocket socket;

    public WebSocketConnector(URLInfo info, HandshakeServer hs) {
        super(info, hs);
    }

    @Override
    public synchronized void connect(final Handler<JsonObject> data,
                                        final Handler<Void> dcHandler,
                                        final boolean sslVerify) {
        client = Utils.VERTX.createHttpClient();
        client.setHost(dataEndpoint.host).setPort(dataEndpoint.port);
        if (dataEndpoint.secure) {
            client.setSSL(true);
            client.setVerifyHost(sslVerify);
        }

        client.connectWebsocket(getPath(), new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket event) {
                event.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        data.handle(new JsonObject(event.toString()));
                    }
                });

                event.closeHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        if (dcHandler != null) {
                            dcHandler.handle(event);
                        }
                    }
                });
            }
        });
    }

    @Override
    public synchronized void disconnect() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public synchronized void write(JsonObject obj) {
        if (socket != null) {
            socket.writeTextFrame(obj.encode());
        }
    }
}
