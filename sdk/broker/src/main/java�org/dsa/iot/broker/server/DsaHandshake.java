package org.dsa.iot.broker.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class DsaHandshake {

    private final Broker broker;

    public DsaHandshake(Broker broker) {
        this.broker = broker;
    }

    public ByteBuf initialize(JsonObject handshake,
                                     String dsId) {
        if (handshake == null) {
            throw new NullPointerException("handshake");
        }
        Client client = Client.registerNewConn(broker, dsId, handshake);
        JsonObject obj = new JsonObject();
        obj.put("tempKey", client.getTempKey().encodedPublicKey());
        obj.put("salt", client.getSalt());

        String path = client.getPath();
        if (path != null) {
            obj.put("path", path);
        }
        obj.put("wsUri", "/ws");
        byte[] bytes = obj.encode().getBytes(CharsetUtil.UTF_8);
        return Unpooled.wrappedBuffer(bytes);
    }
}
