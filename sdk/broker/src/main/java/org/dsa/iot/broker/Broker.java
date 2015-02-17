package org.dsa.iot.broker;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.dsa.iot.dslink.node.Node;

/**
 * @author Samuel Grenier
 */
public class Broker {

    @Getter
    private final EventBus bus;

    @NonNull
    private final DSLink dslink;

    private final Node connections;
    private final Node defs;
    private final Node quarantine;

    public Broker(@NonNull EventBus master,
                  @NonNull DSLink link) {
        this.bus = master;
        this.dslink = link;
        bus.register(this);

        val manager = dslink.getNodeManager();
        connections = manager.createRootNode("conns");
        defs = manager.createRootNode("def");
        quarantine = manager.createRootNode("quarantine");
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
