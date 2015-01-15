package org.dsa.iot.responder.connection;

import org.dsa.iot.core.Utils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;

/**
 * @author Samuel Grenier
 */
class WebSocketConnector extends Connector {

    private HttpClient client;
    private WebSocket socket;

    public WebSocketConnector(String url, boolean secure,
                              Handler<Void> dcHandler) {
        super(url, secure, dcHandler);
    }

    @Override
    public void connect(final Handler<JsonObject> parser) throws IOException {
        client = Utils.VERTX.createHttpClient();
        client.setHost(url.host).setPort(url.port);

        client.connectWebsocket(url.path, new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket ws) {
                socket = ws;

                ws.closeHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        disconnected();
                    }
                });

                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        JsonObject obj = new JsonObject(event.toString());
                        if (isAuthenticated()) {
                            parser.handle(obj);
                        } else {
                            finalizeHandshake(obj);
                        }
                    }
                });

                connected();
            }
        });
    }

    @Override
    public void write(String data) {
        socket.writeTextFrame(data);
    }

    @Override
    public void disconnect() {
        client.close();
    }
}
