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

/**
 * @author Samuel Grenier
 */
public class Broker {

    @Getter
    private final EventBus bus;

    @NonNull
    private final DSLink dslink;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        val bus = new EventBus();
        val hc = HandshakeClient.generate("broker", true, true);
        val link = DSLink.generate(bus, new WebServerConnector(hc));

        Broker broker = new Broker(bus, link);
        broker.listen();
        link.sleep();
    }

    public Broker(@NonNull EventBus master,
                  @NonNull DSLink link) {
        this.bus = master;
        this.dslink = link;
        bus.register(this);
    }

    public void listen() {
        // TODO: Setup configurable ports
        listen(8080);
    }

    public void listen(int port) {
        System.out.println("Listening on port " + port);
        dslink.listen(port);
    }

    public void stop() {
        dslink.stopListening();
    }

    @Subscribe
    public void error(AsyncExceptionEvent event) {
        event.getThrowable().printStackTrace();
    }
}
