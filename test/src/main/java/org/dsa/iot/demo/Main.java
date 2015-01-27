package org.dsa.iot.demo;

import lombok.SneakyThrows;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;

/**
 * @author Samuel Grenier
 */
public class Main {

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        System.out.println("Initializing...");

        final String url = "http://localhost:8080/conn";
        final String endpoint = "ws://localhost:8080";

        DSLink link = DSLink.generate(url, endpoint, ConnectionType.WS, "test", "default");
        link.getResponder().createRoot("Demo");
        link.getResponder().createRoot("Test");

        System.out.println("Connecting...");
        link.connect();
        System.out.println("Connected");
        while (link.isConnected()) {
            Thread.sleep(500);
        }
        System.out.println("Disconnected");
    }
}
