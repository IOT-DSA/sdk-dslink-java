package org.dsa.iot.demo;

import lombok.SneakyThrows;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.Connector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.connection.handshake.HandshakePair;
import org.dsa.iot.dslink.connection.handshake.HandshakeServer;

/**
 * @author Samuel Grenier
 */
public class Main {

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        System.out.println("Initializing...");

        // TODO: handle handshaking in the DSLink class
        int errors = 0;
        for (int i = 0; i < 100; i++) {
            try {
                HandshakeClient client = HandshakeClient.generate("demo");
                HandshakeServer server = HandshakeServer.perform("http://localhost:8080/conn", client);
                HandshakePair pair = new HandshakePair(client, server);
                Connector conn = Connector.create("ws://localhost:8080", pair, ConnectionType.WS);

                DSLink link = new DSLink(conn);
                link.getResponder().createRoot("Demo");
                System.out.println("Connecting...");
                link.connect();
                System.out.println("Connected");
                Thread.sleep(1000);

                System.out.println(i + " ----------------");
            } catch (NullPointerException e) {
                errors++;
            }

        }
        System.out.println("There were " + errors + " errors for retrieving data (this does not count security exceptions)");
    }

}
