package org.dsa.iot.dslink.connection.connector.client;

import com.google.common.eventbus.EventBus;
import org.dsa.iot.core.URLInfo;
import org.dsa.iot.core.Utils;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.dsa.iot.dslink.events.IncomingDataEvent;
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

    private boolean connecting = false;
    private boolean connected = false;

    public WebSocketConnector(EventBus bus, URLInfo info, HandshakePair pair) {
        super(bus, info, pair);
    }

    @Override
    public synchronized void connect(final boolean sslVerify) {
        connecting = true;
        client = Utils.VERTX.createHttpClient();
        client.setHost(getDataEndpoint().host).setPort(getDataEndpoint().port);
        if (getDataEndpoint().secure) {
            client.setSSL(true);
            client.setVerifyHost(sslVerify);
        }

        client.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                getBus().post(new AsyncExceptionEvent(event));
                connecting = false;
                connected = false;
            }
        });

        client.connectWebsocket(getPath(), new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket event) {
                connected = true;
                connecting = false;
                socket = event;

                event.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        JsonObject data = new JsonObject(event.toString());
                        getBus().post(new IncomingDataEvent(data));
                    }
                });

                event.endHandler(getDisconnectHandler());
                event.closeHandler(getDisconnectHandler());
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

    @Override
    public synchronized boolean isConnecting() {
        return connecting;
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
