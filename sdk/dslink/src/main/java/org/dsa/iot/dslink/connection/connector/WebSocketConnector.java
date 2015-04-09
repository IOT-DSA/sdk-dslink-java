package org.dsa.iot.dslink.connection.connector;

import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.util.HttpClientUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;

/**
 * Handles connecting to web socket servers.
 *
 * @author Samuel Grenier
 */
public class WebSocketConnector extends RemoteEndpoint {

    private WebSocket webSocket;

    public WebSocketConnector(DataHandler handler) {
        super(handler);
    }

    @Override
    public void start() {
        HttpClient client = HttpClientUtils.configure(getEndpoint());
        client.connectWebsocket(getUri(), new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket event) {
                webSocket = event;

                Handler<Void> onConnected = getOnConnected();
                if (onConnected != null) {
                    onConnected.handle(null);
                }

                Handler<Throwable> onException = getOnException();
                if (onException != null) {
                    event.exceptionHandler(onException);
                }

                Handler<Buffer> onData = getOnData();
                if (onData != null) {
                    event.dataHandler(onData);
                }

                Handler<Void> onDisconnected = getOnDisconnected();
                if (onDisconnected != null) {
                    event.endHandler(onDisconnected);
                }
            }
        });
    }

    @Override
    public void close() {
        if (webSocket != null) {
            try {
                webSocket.close();
            } catch (IllegalStateException ignored) {
            }

            webSocket = null;
        }
    }

    @Override
    public void write(String data) {
        checkConnected();
        webSocket.writeTextFrame(data);
    }

    private void checkConnected() {
        if (webSocket == null) {
            throw new RuntimeException("Cannot write to unconnected connection");
        }
    }
}
