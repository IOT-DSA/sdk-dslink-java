package org.dsa.iot.broker;

import lombok.val;
import org.dsa.iot.broker.backend.BrokerLink;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.connection.connector.server.connectors.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;

public class Main {

    public static void main(String[] args) {
        val bus = EventBusFactory.create();
        val hc = HandshakeClient.generate("broker", true, true);
        val conn = new WebServerConnector(bus, hc);
        val link = BrokerLink.create(bus, conn);
        Broker broker = new Broker(link);
        broker.listenAndConnect();
        broker.sleep();
    }
}
