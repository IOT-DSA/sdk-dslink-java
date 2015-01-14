package org.dsa.iot.responder.connection;

import org.dsa.iot.core.Utils;
import org.dsa.iot.responder.Responder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.net.URL;

/**
 * @author Samuel Grenier
 */
class WebSocketConnector extends Connector {

    private HttpClient client;

    public WebSocketConnector(String url, boolean secure,
                              Handler<Void> dcHandler) {
        super(url, secure, dcHandler);
    }

    public void connect(final Handler<JsonObject> parser) throws IOException {
        URL url = new URL(this.url);

        String host = url.getHost();
        int port = url.getPort();
        if (port == -1)
            port = Utils.getDefaultPort(url.getProtocol());

        String path = url.getPath();
        if (path == null || path.isEmpty())
            path = "/";

        client = Utils.VERTX.createHttpClient();
        client.setHost(host).setPort(port);
        client.connectWebsocket(path, new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket ws) {
                ws.closeHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        disconnected();
                    }
                });

                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        parser.handle(new JsonObject(event.toString()));
                    }
                });
            }
        });
    }

    @Override
    public void disconnect() {
        client.close();
    }
}
