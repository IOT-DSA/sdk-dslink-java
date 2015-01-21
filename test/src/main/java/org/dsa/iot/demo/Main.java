package org.dsa.iot.demo;

import lombok.SneakyThrows;
import org.dsa.iot.responder.Responder;
import org.dsa.iot.responder.connection.ConnectionType;
import org.dsa.iot.responder.connection.Connector;
import org.dsa.iot.responder.connection.handshake.HandshakeClient;
import org.dsa.iot.responder.connection.handshake.HandshakeServer;

/**
 * @author Samuel Grenier
 */
public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Initializing...");
        Responder resp = new Responder();

        HandshakeClient client = HandshakeClient.generate("demo");
        HandshakeServer server = HandshakeServer.perform("http://localhost:8080/conn", client);

        resp.createRoot("Demo");

        resp.setConnector(Connector.create("ws://localhost:8080", server, ConnectionType.WS));
        resp.connect();
        System.out.println("Connected");
    }

}
