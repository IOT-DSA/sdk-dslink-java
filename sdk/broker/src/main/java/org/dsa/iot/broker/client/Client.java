package org.dsa.iot.broker.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.server.DsaHandshake;
import org.dsa.iot.broker.utils.Dispatch;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Samuel Grenier
 */
public class Client extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final MessageProcessor processor = new MessageProcessor(this);
    private final DsaHandshake handshake;
    private final AtomicInteger rid;
    private final Broker broker;

    private ChannelHandlerContext ctx;

    public Client(Broker broker, DsaHandshake handshake) {
        if (broker == null) {
            throw new NullPointerException("broker");
        } else if (handshake == null) {
            throw new NullPointerException("handshake");
        }
        this.broker = broker;
        this.handshake = handshake;
        if (handshake.isResponder()) {
            this.rid = new AtomicInteger();
        } else {
            this.rid = null;
        }
    }

    public int nextRid() {
        return rid.incrementAndGet();
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
        }
        broker().getClientManager().clientDisconnected(this);
    }

    public void write(String data) {
        byte[] bytes = data.getBytes(CharsetUtil.UTF_8);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        TextWebSocketFrame frame = new TextWebSocketFrame(buf);
        ctx.channel().writeAndFlush(frame);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        close();
    }

    public void addDispatch(int remoteRid, Dispatch dispatch) {
        processor.addDispatch(remoteRid, dispatch);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
                                   WebSocketFrame frame) throws Exception {
        final Channel channel = ctx.channel();
        if (frame instanceof TextWebSocketFrame) {
            String data = ((TextWebSocketFrame) frame).text();
            JsonObject obj = new JsonObject(data);
            processor.processData(obj);
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
