package org.dsa.iot.broker;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.events.AsyncExceptionEvent;
import org.dsa.iot.dslink.events.ClientConnectedEvent;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.events.RequestEvent;
import org.dsa.iot.dslink.node.Node;

/**
 * @author Samuel Grenier
 */
public class Broker {

    @Getter
    private final MBassador<Event> bus;

    @NonNull
    private final DSLink dslink;

    private final Node connections;
    private final Node defs;
    private final Node quarantine;

    public Broker(@NonNull MBassador<Event> master,
                  @NonNull DSLink link) {
        this.bus = master;
        this.dslink = link;
        bus.subscribe(this);

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

    @Handler
    public void onConnected(ClientConnectedEvent event) {
        connections.createChild(event.getClient().getDsId());
    }
    
    @Handler
    public void error(AsyncExceptionEvent event) {
        event.getThrowable().printStackTrace();
    }
}
