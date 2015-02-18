package org.dsa.iot.dslink;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.*;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.dsa.iot.dslink.connection.handshake.HandshakeServer;
import org.dsa.iot.dslink.events.IncomingDataEvent;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.responder.Responder;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import java.util.concurrent.CountDownLatch;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    @Getter
    private final EventBus bus;

    private final ClientConnector clientConnector;
    private final ServerConnector serverConnector;

    @Getter
    private final Requester requester;

    @Getter
    private final Responder responder;

    private DSLink(EventBus bus,
                    ClientConnector clientConn,
                    ServerConnector serverConn,
                    Requester req,
                    Responder resp) {
        this.bus = bus;
        this.clientConnector = clientConn;
        this.serverConnector = serverConn;
        this.requester = req;
        this.responder = resp;
        bus.register(this);

        SubscriptionManager subManager = null;
        if (clientConn != null)
            subManager = new SubscriptionManager(clientConn);
        NodeManager common = new NodeManager(bus, subManager);
        if (requester != null)
            requester.setConnector(clientConn, serverConn, common);
        if (responder != null)
            responder.setConnector(clientConn, serverConn, common);

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
        serverConnector.stop();
    }

    public boolean isConnecting() {
        return clientConnector != null && clientConnector.isConnecting();
    }

    public boolean isConnected() {
        return clientConnector != null && clientConnector.isConnected();
    }

    public void connect() {
        connect(true);
    }

    public void connect(boolean sslVerify) {
        checkConnected();
        clientConnector.connect(sslVerify);
    }

    public void disconnect() {
        if (clientConnector.isConnected()) {
            clientConnector.disconnect();
        }
    }

    public NodeManager getNodeManager() {
        if (requester != null)
            return requester.getManager();
        else if (responder != null)
            return responder.getManager();
        else
            return null;
    }

    /**
     * Blocks the thread until the link is disconnected or the server is
     * stopped.
     */
    @SneakyThrows
    public void sleep() {
        while (isConnecting() || isConnected() || isListening()) {
            Thread.sleep(500);
        }
    }

    @Subscribe
    public void jsonHandler(IncomingDataEvent event) {
        try {
            val data = event.getData();
            if (responder != null) {
                val array = data.getArray("requests");
                if (array != null) {
                    responder.parse(event.getClient(), array);
                }
            }
            if (requester != null) {
                val array = data.getArray("responses");
                if (array != null) {
                    requester.parse(event.getClient(), array);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public static DSLink generate(EventBus master,
                                String url,
                                ConnectionType type,
                                String dsId) {
        return generate(master, url, type, dsId, "default");
    }

    /**
     * Defaults to generating a responder only dslink.
     */
    public static DSLink generate(EventBus master,
                                String url,
                                ConnectionType type,
                                String dsId,
                                String zone) {
        return generate(master, url, type, dsId, zone, false, true);
    }

    public static DSLink generate(EventBus master,
                                  String url,
                                  ConnectionType type,
                                  String dsId,
                                  boolean isRequester,
                                  boolean isResponder) {
        return generate(master, url, type, dsId, "default",
                        isRequester, isResponder);
    }

    public static DSLink generate(EventBus master,
                                String url,
                                ConnectionType type,
                                String dsId,
                                String zone,
                                boolean isRequester,
                                boolean isResponder) {
        val requester = isRequester ? new Requester(master) : null;
        val responder = isResponder ? new Responder(master) : null;

        return generate(master, url, type, dsId, zone,
                    requester, responder);
    }

    /**
     *
     * @param url URL to perform the handshake authentication
     * @param type Type of connection to use
     * @param zone Quarantine zone to use
     * @param requester Requester instance to use, otherwise null
     * @param responder Responder instance to use, otherwise null
     * @return DSLink object on success, otherwise null
     */
    @SneakyThrows
    public static DSLink generate(@NonNull final EventBus master,
                                @NonNull final String url,
                                @NonNull final ConnectionType type,
                                @NonNull final String dsId,
                                @NonNull final String zone,
                                final Requester requester,
                                final Responder responder) {
        // TODO: overload for custom handshake client
        val client = HandshakeClient.generate(dsId, zone,
                                            requester != null, responder != null);

        final CountDownLatch latch = new CountDownLatch(1);
        final HandshakeCont server = new HandshakeCont();
        HandshakeServer.perform(master, url, client, new Handler<AsyncResult<HandshakeServer>>() {
            @Override
            public void handle(AsyncResult<HandshakeServer> event) {
                server.setServer(event);
                latch.countDown();
            }
        });
        latch.await();
        if (server.getServer().failed()) {
            throw new RuntimeException(server.getServer().cause());
        }

        val pair = new HandshakePair(client, server.getServer().result());
        val conn = ClientConnector.create(master, url, pair, type);
        return new DSLink(master, conn, null, requester, responder);
    }

    public static DSLink generate(@NonNull final EventBus master,
                                @NonNull final ServerConnector connector) {
        return new DSLink(master, null,
                                    connector,
                                    new Requester(master),
                                    new Responder(master));
    }

    private static class HandshakeCont {

        @Getter
        @Setter
        private AsyncResult<HandshakeServer> server;
    }
}
