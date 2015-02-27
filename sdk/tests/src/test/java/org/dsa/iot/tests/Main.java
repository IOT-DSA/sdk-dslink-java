package org.dsa.iot.tests;

import lombok.SneakyThrows;
import lombok.val;
import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.backend.BrokerLink;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.connector.server.connectors.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.ServerSocket;

/**
 * @author Samuel Grenier
 */
public class Main {

    private static Broker broker;
    private static int port;

    @BeforeClass
    public static void setup() {
        val bus = EventBusFactory.create();
        val client = HandshakeClient.generate("broker", "_", true, true);
        val conn = new WebServerConnector(bus, client);
        val link = BrokerLink.create(bus, conn);
        broker = new Broker(link);
        port = getRandomPort();
        broker.listen(port);
    }

    @AfterClass
    public static void tearDown() {
        broker.stop();
    }

    @Test
    @SneakyThrows
    public void auth() {
        val bus = EventBusFactory.create();
        val url = "http://localhost:" + port + "/conn";

        val link = DSLinkFactory.create().generate(bus, url, ConnectionType.WS, "dslink");
        link.connect();
        while (link.isConnecting()) {
            Thread.sleep(100);
        }

        Assert.assertTrue(link.isConnected());
        link.disconnect();
    }

    @SneakyThrows
    private static int getRandomPort() {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
