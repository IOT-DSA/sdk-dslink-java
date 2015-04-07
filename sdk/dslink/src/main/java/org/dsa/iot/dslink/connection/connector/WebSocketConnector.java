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
    private boolean isActive;

    public WebSocketConnector(DataHandler handler) {
        super(handler);
    }

    @Override
    public void start(final Handler<Void> onConnected) {
        HttpClient client = HttpClientUtils.configure(getEndpoint());
        client.connectWebsocket(getUri(), new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket event) {
                webSocket = event;
                isActive = true;
                if (onConnected != null) {
                    onConnected.handle(null);
                }

                event.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        event.printStackTrace();
                    }
                });

                event.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        getDataHandler().processData(event);
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
    public void close() {
        if (webSocket != null) {
            webSocket.close();
        }
    }

    @Override
    public void write(String data) {
        checkConnected();
        webSocket.writeTextFrame(data);
    }

    private void checkConnected() {
        if (!isActive) {
            throw new RuntimeException("Cannot write to unconnected connection");
        }
    }
}
