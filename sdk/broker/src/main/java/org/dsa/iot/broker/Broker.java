package org.dsa.iot.broker;

import lombok.NonNull;
import org.dsa.iot.broker.backend.BrokerLink;

/**
 * @author Samuel Grenier
 */
public class Broker {

    private final BrokerLink link;

    public Broker(@NonNull BrokerLink link) {
        this.link = link;
    }

    public void listen() {
        // TODO: Setup configurable ports
        listen(8080);
    }

    public void listen(int port) {
        System.out.println("Listening on port " + port);
        link.listen(port);
    }

    public void stop() {
        link.stopListening();
    }
    
    public void sleep() {
        link.sleep();
    }
}
