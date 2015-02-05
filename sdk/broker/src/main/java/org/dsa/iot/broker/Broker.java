package org.dsa.iot.broker;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.connector.server.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
@RequiredArgsConstructor
public class Broker {

    @NonNull
    private final DSLink dslink;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws InterruptedException {
        HandshakeClient hc = HandshakeClient.generate("broker", true, true);
        DSLink.generate(new WebServerConnector(hc), new Handler<DSLink>() {
            @Override
            public void handle(DSLink event) {
                Broker broker = new Broker(event);
                broker.listen();
            }
        });

        while (true) {
            Thread.sleep(500);
        }
    }

    public void listen() {
        // TODO: Setup configurable ports
        System.out.println("Listening on port 8080");
        dslink.listen(8080);
    }
}
