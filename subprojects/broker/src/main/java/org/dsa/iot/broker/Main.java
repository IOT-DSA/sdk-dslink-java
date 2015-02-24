package org.dsa.iot.broker;

import lombok.val;
import org.dsa.iot.broker.overrides.BrokerRequester;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.connector.server.connectors.WebServerConnector;
import org.dsa.iot.dslink.connection.handshake.HandshakeClient;
import org.dsa.iot.dslink.responder.Responder;

public class Main {

    public static void main(String[] args) {
        val bus = EventBusFactory.create();
        val hc = HandshakeClient.generate("broker", true, true);
        val conn = new WebServerConnector(bus, hc);
        val req = new BrokerRequester(bus);
        val resp = new Responder(bus);
        val link = DSLinkFactory.create().generate(bus, null, conn, req, resp);

        Broker broker = new Broker(bus, link);
        broker.listen();
        link.sleep();
    }
}
