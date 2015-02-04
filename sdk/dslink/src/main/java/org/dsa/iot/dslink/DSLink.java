package org.dsa.iot.dslink;

import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.ServerConnector;
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

    private final ClientConnector clientConnector;
    private final ServerConnector serverConnector;

    private final Requester requester;

    @Getter
    private final Responder responder;

    private DSLink(ClientConnector clientConn,
                   ServerConnector serverConn,
                    Requester req,
                    Responder resp) {
        this.clientConnector = clientConn;
        this.serverConnector = serverConn;
        this.requester = req;
        this.responder = resp;
        if (requester != null)
            requester.setConnector(clientConn);
        if (responder != null)
            responder.setConnector(clientConn);
    }

    public boolean isListening() {
        return serverConnector != null && serverConnector.isListening();
    }

    public void listen(int port) {
        listen(port, "0.0.0.0");
    }

    public void listen(int port, @NonNull String bindAddr) {
        // TODO: SSL support
        checkListening();
        serverConnector.start(port, bindAddr);
    }

    public void stopListening() {
        checkListening();
        serverConnector.stop();
    }

    public boolean isConnected() {
        return clientConnector != null && clientConnector.isConnected();
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
        clientConnector.setExceptionHandler(exceptionHandler);
        clientConnector.connect(new Handler<JsonObject>() {
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
        if (clientConnector.isConnected()) {
            clientConnector.disconnect();
        }
    }

    private void checkConnected() {
        if (clientConnector == null) {
            throw new IllegalStateException("No client connector implementation provided");
        } else if (clientConnector.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
    }

    private void checkListening() {
        if (serverConnector == null) {
            throw new IllegalStateException("No server connector implementation provided");
        } else if (serverConnector.isListening()) {
            throw new IllegalStateException("Already listening");
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
                onComplete.handle(new DSLink(conn, null, requester, responder));
            }
        }, exceptionHandler);
    }

    public static void generate(@NonNull final ServerConnector connector,
                                @NonNull final Handler<DSLink> onComplete) {
        onComplete.handle(new DSLink(null, connector, new Requester(), new Responder()));
    }
}
