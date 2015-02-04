package org.dsa.iot.demo;

import lombok.SneakyThrows;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Main {

    private static boolean running = true;
    private static DSLink link;

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        System.out.println("Initializing...");

        final String url = "http://localhost:8080/conn";
        final String endpoint = "ws://localhost:8080";

        DSLink.generate(url, endpoint, ConnectionType.WS, "test", new Handler<DSLink>() {
            @Override
            @SneakyThrows
            public void handle(DSLink link) {
                Main.link = link;
                link.getResponder().createRoot("Demo");
                link.getResponder().createRoot("Test");

                System.out.println("Connecting...");
                link.connect(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        System.err.println("A fatal error has occurred during the data endpoint connection");
                        event.printStackTrace();
                        running = false;
                    }
                });
                System.out.println("Connected");
            }
        }, new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                System.err.println("A fatal error has occurred during the handshake");
                event.printStackTrace();
                running = false;
            }
        });

        while (running && (link == null || link.isConnected())) {
            Thread.sleep(500);
        }
        System.out.println("Disconnected");
    }
}
