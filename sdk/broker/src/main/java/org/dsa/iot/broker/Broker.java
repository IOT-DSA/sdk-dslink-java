package org.dsa.iot.broker;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
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

    @Getter
    private final EventBus bus;

    @NonNull
    private final DSLink dslink;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws InterruptedException {
        final EventBus bus = new EventBus();
        final HandshakeClient hc = HandshakeClient.generate("broker", true, true);
        DSLink.generate(bus, new WebServerConnector(hc), new Handler<DSLink>() {
            @Override
            public void handle(DSLink event) {
                Broker broker = new Broker(bus, event);
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
