package org.dsa.iot.broker;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.connector.server.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Broker {

    @Getter
    private final EventBus bus;

    @NonNull
    private final DSLink dslink;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws InterruptedException {
        final EventBus bus = new EventBus();
        val hc = HandshakeClient.generate("broker", true, true);
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

    public Broker(@NonNull EventBus master,
                  @NonNull DSLink link) {
        this.bus = master;
        this.dslink = link;
        bus.register(this);
    }

    public void listen() {
        // TODO: Setup configurable ports
        System.out.println("Listening on port 8080");
        dslink.listen(8080);
    }

    @Subscribe
    public void error(AsyncExceptionEvent event) {
        event.getThrowable().printStackTrace();
    }
}
