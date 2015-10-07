package org.dsa.iot.broker.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.dslink.util.json.JsonObject;

/**
 * @author Samuel Grenier
 */
public class DsaHandshake {

    public static ByteBuf createHandshake(Client client) {
        JsonObject obj = new JsonObject();
        obj.put("tempKey", client.getTempKey().encodedPublicKey());
        obj.put("salt", client.getSalt());
        obj.put("path", "/downstream/" + client.getName()); // TODO: handle duplicates
        obj.put("wsUri", "/ws");
        byte[] bytes = obj.encode().getBytes(CharsetUtil.UTF_8);
        return Unpooled.wrappedBuffer(bytes);
    }
}
