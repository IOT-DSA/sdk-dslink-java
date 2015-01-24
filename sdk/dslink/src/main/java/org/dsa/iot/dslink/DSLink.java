package org.dsa.iot.dslink;

import lombok.Getter;
import lombok.NonNull;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.Connector;
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

    private final Connector connector;

    private final Requester requester;

    @Getter
    private final Responder responder;

    public DSLink(Connector conn) {
        this(conn, new Responder());
    }

    public DSLink(Connector conn, Requester req) {
        this(conn, req, null);
    }

    public DSLink(Connector conn, Responder resp) {
        this(conn, null, resp);
    }

    public DSLink(@NonNull Connector conn,
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
        connect(true);
    }

    public void connect(boolean sslVerify) {
        checkConnected();
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

    /**
     *
     * @param url URL to perform the handshake authentication
     * @param endpoint Data endpoint URL
     * @param type Type of connection to use
     * @param zone Quarantine zone to use
     * @return A factory created DSLink object
     */
    public static DSLink generate(@NonNull String url,
                                  @NonNull String endpoint,
                                  @NonNull ConnectionType type,
                                  @NonNull String zone) {
        HandshakeClient client = HandshakeClient.generate(zone);
        HandshakeServer server = HandshakeServer.perform(url, client);
        HandshakePair pair = new HandshakePair(client, server);
        Connector conn = Connector.create(endpoint, pair, type);
        return new DSLink(conn);
    }
}
