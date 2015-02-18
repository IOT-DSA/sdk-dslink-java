package org.dsa.iot.broker;

import com.google.common.eventbus.EventBus;
import lombok.val;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.connection.connector.server.connectors.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;

public class Main {

    public static void main(String[] args) {
        val bus = new EventBus();
        val hc = HandshakeClient.generate("broker", true, true);
        val link = DSLink.generate(bus, new WebServerConnector(bus, hc));

        Broker broker = new Broker(bus, link);
        broker.listen();
        link.sleep();
    }
}
