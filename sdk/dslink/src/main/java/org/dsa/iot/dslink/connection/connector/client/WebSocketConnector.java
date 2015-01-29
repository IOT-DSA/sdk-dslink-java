package org.dsa.iot.dslink.connection.connector.client;

import org.dsa.iot.core.URLInfo;
import org.dsa.iot.core.Utils;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class WebSocketConnector extends ClientConnector {

    protected HttpClient client;
    protected WebSocket socket;

    private boolean connected = false;

    public WebSocketConnector(URLInfo info, HandshakePair pair) {
        super(info, pair);
    }

    @Override
    public synchronized void connect(final Handler<JsonObject> data,
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
                socket = event;

                event.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        data.handle(new JsonObject(event.toString()));
                    }
                });

                event.endHandler(getDisconnectHandler());
                event.closeHandler(getDisconnectHandler());
            }
        });

        connected = true;
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

    @Override
    public synchronized boolean isConnected() {
        return connected;
    }

    private Handler<Void> getDisconnectHandler() {
        return new Handler<Void>() {
            @Override
            public void handle(Void event) {
                synchronized (WebSocketConnector.this) {
                    connected = false;
                }
            }
        };
    }
}
