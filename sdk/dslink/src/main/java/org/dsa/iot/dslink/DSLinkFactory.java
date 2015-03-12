package org.dsa.iot.dslink;

import lombok.*;
import net.engio.mbassy.bus.MBassador;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.dsa.iot.dslink.connection.handshake.HandshakeServer;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.responder.Responder;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import java.util.concurrent.CountDownLatch;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DSLinkFactory {

    private final MBassador<Event> bus;

    public static DSLinkFactory create(MBassador<Event> bus) {
        return new DSLinkFactory(bus);
    }
    
    public DSLink generate(String url,
                                  ConnectionType type,
                                  String dsId) {
        return generate(url, type, dsId, "default");
    }

    /**
     * Defaults to generating a responder only dslink.
     */
    public DSLink generate(String url,
                                  ConnectionType type,
                                  String dsId,
                                  String zone) {
        return generate(url, type, dsId, zone, false, true);
    }

    public DSLink generate(String url,
                                  ConnectionType type,
                                  String dsId,
                                  boolean isRequester,
                                  boolean isResponder) {
        return generate(url, type, dsId, "default",
                isRequester, isResponder);
    }

    public DSLink generate(String url,
                                  ConnectionType type,
                                  String dsId,
                                  String zone,
                                  boolean isRequester,
                                  boolean isResponder) {
        val requester = isRequester ? new Requester(bus) : null;
        val responder = isResponder ? new Responder(bus) : null;

        return generate(url, type, dsId, zone,
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
    public DSLink generate(@NonNull final String url,
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
        HandshakeServer.perform(bus, url, client, new Handler<AsyncResult<HandshakeServer>>() {
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
        val conn = ClientConnector.create(bus, url, pair, type);
        return new DSLink(bus, conn, null, requester, responder);
    }

    public DSLink generate(@NonNull MBassador<Event> master,
                           ClientConnector clientConnector,
                           ServerConnector serverConnector,
                           Requester requester,
                           Responder responder) {
        return new DSLink(master, clientConnector,
                            serverConnector, requester, responder);
    }

    private static class HandshakeCont {

        @Getter
        @Setter
        private AsyncResult<HandshakeServer> server;
    }
}
