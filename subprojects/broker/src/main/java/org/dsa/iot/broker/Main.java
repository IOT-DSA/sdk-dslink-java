package org.dsa.iot.broker;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.dsa.iot.broker.backend.BrokerLink;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.connector.server.connectors.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.dsa.iot.dslink.connection.handshake.HandshakeServer;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import java.util.concurrent.CountDownLatch;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        boolean client = false;
        if (args.length > 0 && "-client".equals(args[0])) {
            // TODO: switch to the arg manager
            client = true;
        }

        val bus = EventBusFactory.create();
        val hc = HandshakeClient.generate("broker", true, true);
        val serverConn = new WebServerConnector(bus, hc);
        ClientConnector clientConn = null;
        if (client) {
            val url = "http://localhost:8080/conn";
            val h = new HandshakeHandler();
            HandshakeServer.perform(bus, url, hc, h);
            h.getLatch().await();
            val pair = new HandshakePair(hc, h.getServer());
            val type = ConnectionType.WS;
            clientConn = ClientConnector.create(bus, url, pair, type);
        }

        val link = BrokerLink.create(bus, serverConn, clientConn);
        Broker broker = new Broker(link);
        broker.listenAndConnect(8081);
        broker.sleep();
    }

    private static class HandshakeHandler
                            implements Handler<AsyncResult<HandshakeServer>> {

        @Getter
        private final CountDownLatch latch = new CountDownLatch(1);

        @Getter
        private HandshakeServer server;

        @Override
        public void handle(AsyncResult<HandshakeServer> event) {
            if (event.failed()) {
                throw new RuntimeException(event.cause());
            }
            server = event.result();
            latch.countDown();
        }
    }
}
