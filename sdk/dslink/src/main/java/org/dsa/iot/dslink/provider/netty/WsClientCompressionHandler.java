package org.dsa.iot.dslink.provider.netty;

import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateClientExtensionHandshaker;

public class WsClientCompressionHandler extends WebSocketClientExtensionHandler {

    public WsClientCompressionHandler() {
        super(new PerMessageDeflateClientExtensionHandshaker(6, true, 15, false, false),
                new DeflateFrameClientExtensionHandshaker(false),
                new DeflateFrameClientExtensionHandshaker(true));
    }
}
