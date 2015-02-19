package org.dsa.iot.broker;

import lombok.val;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.connector.server.connectors.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;

public class Main {

    public static void main(String[] args) {
        val bus = EventBusFactory.create();
        val hc = HandshakeClient.generate("broker", true, true);
        val link = DSLink.generate(bus, new WebServerConnector(bus, hc));

        Broker broker = new Broker(bus, link);
        broker.listen();
        link.sleep();
    }
}
