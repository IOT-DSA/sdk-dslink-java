package org.dsa.iot.tests;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import lombok.val;
import org.dsa.iot.broker.Broker;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.connector.server.WebServerConnector;
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
    @SneakyThrows
    public static void setup() {
        val bus = new EventBus();
        val client = HandshakeClient.generate("broker", "_", true, true);
        val link = DSLink.generate(bus, new WebServerConnector(client));
        broker = new Broker(bus, link);
        port = getRandomPort();
        broker.listen(port);
        // Ensure the server is listening
        Thread.sleep(100);
    }

    @AfterClass
    public static void tearDown() {
        broker.stop();
    }

    @Test
    @SneakyThrows
    public void auth() {
        val bus = new EventBus();
        val url = "http://localhost:" + port + "/conn";

        val link = DSLink.generate(bus, url, ConnectionType.WS, "dslink");
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
