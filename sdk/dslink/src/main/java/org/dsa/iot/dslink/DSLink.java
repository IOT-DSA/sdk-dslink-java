package org.dsa.iot.dslink;

import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.dsa.iot.dslink.connection.handshake.HandshakeServer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    private final ClientConnector connector;

    private final Requester requester;

    @Getter
    private final Responder responder;

    public DSLink(@NonNull ClientConnector conn,
                  Requester req,
                  Responder resp) {
        this.connector = conn;
        this.requester = req;
        this.responder = resp;
        if (requester != null)
            requester.setConnector(conn);
        if (responder != null)
            responder.setConnector(conn);
    }

    public boolean isConnected() {
        return connector.isConnected();
    }

    public void connect() {
        connect(null);
    }

    public void connect(Handler<Throwable> exceptionHandler) {
        connect(true, exceptionHandler);
    }

    public void connect(boolean sslVerify,
                        Handler<Throwable> exceptionHandler) {
        checkConnected();
        connector.setExceptionHandler(exceptionHandler);
        connector.connect(new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                if (responder != null) {
                    JsonArray array = event.getArray("requests");
                    if (array != null) {
                        responder.parse(array);
                    }
                }
                if (requester != null) {
                    JsonArray array = event.getArray("responses");
                    if (array != null) {
                        requester.parse(array);
                    }
                }
            }
        }, sslVerify);
    }

    public void disconnect() {
        if (connector.isConnected()) {
            connector.disconnect();
        }
    }

    private void checkConnected() {
        if (connector.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
    }

    public static void generate(String url,
                                String endpoint,
                                ConnectionType type,
                                String dsId,
                                Handler<DSLink> onComplete) {
        generate(url, endpoint, type, dsId, "default", onComplete, null);
    }

    public static void generate(String url,
                                String endpoint,
                                ConnectionType type,
                                String dsId,
                                Handler<DSLink> onComplete,
                                Handler<Throwable> exceptionHandler) {
        generate(url, endpoint, type, dsId, "default", onComplete, exceptionHandler);
    }

    /**
     * Defaults to generating a responder only dslink.
     */
    public static void generate(String url,
                                String endpoint,
                                ConnectionType type,
                                String dsId,
                                String zone,
                                Handler<DSLink> onComplete,
                                Handler<Throwable> exceptionHandler) {
        generate(url, endpoint, type, dsId, zone, false, true, onComplete, exceptionHandler);
    }

    public static void generate(String url,
                                  String endpoint,
                                  ConnectionType type,
                                  String dsId,
                                  String zone,
                                  boolean isRequester,
                                  boolean isResponder,
                                  Handler<DSLink> onComplete,
                                  Handler<Throwable> exceptionHandler) {
        Requester requester = isRequester ? new Requester() : null;
        Responder responder = isResponder ? new Responder() : null;

        generate(url, endpoint, type, dsId, zone, requester, responder, onComplete, exceptionHandler);
    }

    /**
     *
     * @param url URL to perform the handshake authentication
     * @param endpoint Data endpoint URL
     * @param type Type of connection to use
     * @param zone Quarantine zone to use
     * @param requester Requester instance to use, otherwise null
     * @param responder Responder instance to use, otherwise null
     * @param onComplete Callback when a DSLink is generated and authenticated to a server
     */
    public static void generate(@NonNull final String url,
                                  @NonNull final String endpoint,
                                  @NonNull final ConnectionType type,
                                  @NonNull final String dsId,
                                  @NonNull final String zone,
                                  final Requester requester,
                                  final Responder responder,
                                  @NonNull final Handler<DSLink> onComplete,
                                  final Handler<Throwable> exceptionHandler) {
        final HandshakeClient client = HandshakeClient.generate(dsId, zone,
                                            requester != null, responder != null);
        HandshakeServer.perform(url, client, new Handler<HandshakeServer>() {
            @Override
            public void handle(HandshakeServer event) {
                HandshakePair pair = new HandshakePair(client, event);
                ClientConnector conn = ClientConnector.create(endpoint, pair, type);
                onComplete.handle(new DSLink(conn, requester, responder));
            }
        }, exceptionHandler);
    }
}
