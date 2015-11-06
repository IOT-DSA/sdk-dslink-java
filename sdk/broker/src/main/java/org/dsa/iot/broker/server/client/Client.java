package org.dsa.iot.broker.server.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.MessageProcessor;
import org.dsa.iot.broker.server.DsaHandshake;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class Client extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    private final DsaHandshake handshake;
    private final Broker broker;

    private ChannelHandlerContext ctx;
    private DSLinkNode node;

    public Client(Broker broker, DsaHandshake handshake) {
        if (broker == null) {
            throw new NullPointerException("broker");
        } else if (handshake == null) {
            throw new NullPointerException("handshake");
        }
        this.broker = broker;
        this.handshake = handshake;
    }

    public void node(DSLinkNode node) {
        this.node = node;
    }

    public DSLinkNode node() {
        return node;
    }

    public MessageProcessor processor() {
        return node().processor();
    }

    public DsaHandshake handshake() {
        return handshake;
    }

    public Broker broker() {
        return broker;
    }

    public void close() {
        if (ctx != null) {
            ctx.close();
            ctx = null;
            broker().getClientManager().clientDisconnected(this);
        }
    }

    public void write(String data) {
        byte[] bytes = data.getBytes(CharsetUtil.UTF_8);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        TextWebSocketFrame frame = new TextWebSocketFrame(buf);
        ctx.channel().writeAndFlush(frame);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Sent] {}: {}", handshake().dsId(), data);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        write("{}");
        broker.getClientManager().clientConnected(this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        close();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
                                   WebSocketFrame frame) throws Exception {
        final Channel channel = ctx.channel();
        if (frame instanceof TextWebSocketFrame) {
            String data = ((TextWebSocketFrame) frame).text();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[Received] {}: {}", handshake().dsId(), data);
            }
            JsonObject obj = new JsonObject(data);
            processor().processData(obj);
        } else if (frame instanceof PingWebSocketFrame) {
            ByteBuf buf = frame.content().retain();
            channel.writeAndFlush(new PongWebSocketFrame(buf));
        } else if (frame instanceof CloseWebSocketFrame) {
            ChannelPromise prom = channel.newPromise();
            ChannelFutureListener cfl = ChannelFutureListener.CLOSE;
            channel.writeAndFlush(frame.retain(), prom).addListener(cfl);
        } else {
            String err = "%s frame types not supported";
            err = String.format(err, frame.getClass().getName());
            throw new UnsupportedOperationException(err);
        }
    }
}
