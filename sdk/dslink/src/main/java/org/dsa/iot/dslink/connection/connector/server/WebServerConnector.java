package org.dsa.iot.dslink.connection.connector.server;

import org.dsa.iot.core.Utils;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

/**
 * Handles web sockets and HTTP connections.
 * @author Samuel Grenier
 */
public class WebServerConnector extends ServerConnector {

    @Override
    public void start(int port, String bindAddr) {
        HttpServer server = Utils.VERTX.createHttpServer();
        server.requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                HttpServerResponse resp = event.response();
                resp.setStatusCode(200);
                if (!event.method().equals("POST")) {
                    resp.setStatusCode(405); // Method not allowed
                } else if (event.path().equals("/conn")) {
                    JsonObject obj = new JsonObject();

                    // TODO
                    String dsId;
                    String publicKey;
                    String wsUri;
                    String httpUri;
                    String encryptedNonce;
                    String salt;
                    String saltS;
                    int updateInterval;

                    resp.write(obj.encode(), "UTF-8");
                } else {
                    // TODO: handle ws and http endpoints
                    resp.setStatusCode(404); // Page not found
                }
                resp.end();
            }
        });

        server.websocketHandler(new Handler<ServerWebSocket>() {
            @Override
            public void handle(ServerWebSocket event) {

            }
        });

        if (bindAddr != null)
            server.listen(port, bindAddr);
        else
            server.listen(port);
    }

    @Override
    public void stop() {

    }
}
