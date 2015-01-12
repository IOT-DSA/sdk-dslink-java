package org.dsa.iot.responder.connection;

import org.dsa.iot.core.Utils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.WebSocket;

import java.io.IOException;

/**
 * @author Samuel Grenier
 */
class WebSocketConnector extends Connector {

    public WebSocketConnector(String url, boolean secure) {
        super(url, secure);
    }

    public void connect() throws IOException {
        Utils.VERTX.createHttpClient().connectWebsocket(url, new Handler<WebSocket>() {
            @Override
            public void handle(WebSocket event) {
                // TODO
            }
        });
    }
}
