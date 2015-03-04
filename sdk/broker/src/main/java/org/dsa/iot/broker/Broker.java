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

    public void listenAndConnect() {
        listenAndConnect(true);
    }

    public void listenAndConnect(int port) {
        listenAndConnect(port, true);
    }

    public void listenAndConnect(boolean sslVerify) {
        listenAndConnect(8080, sslVerify);
    }

    public void listenAndConnect(int port, boolean sslVerify) {
        System.out.println("Listening on port " + port);
        link.listen(port);
        if (link.hasClientConnector()) {
            link.connect(sslVerify);
        }
    }

    public void stop() {
        link.stopListening();
    }
    
    public void sleep() {
        link.sleep();
    }
}
