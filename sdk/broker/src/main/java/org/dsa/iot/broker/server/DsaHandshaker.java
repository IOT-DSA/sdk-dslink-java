package org.dsa.iot.broker.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Samuel Grenier
 */
public class DsaHandshaker {

    private static final ByteBuf BUF = Unpooled.wrappedBuffer(new byte[0]);

    public static ByteBuf createHandshake() {
        return BUF;
    }
}
