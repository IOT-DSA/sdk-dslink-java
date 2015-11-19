package org.dsa.iot.broker.server.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.node.DSLinkNode;
import org.dsa.iot.broker.processor.MessageProcessor;
import org.dsa.iot.broker.server.DsaHandshake;
import org.dsa.iot.dslink.util.json.JsonArray;
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

    private JsonArray requestsCache;
    private JsonArray responsesCache;

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

    public MessageProcessor processor() {
        return node.processor();
    }

    public DsaHandshake handshake() {
        return handshake;
    }

    public Broker broker() {
        return broker;
    }

    public void close() {
        ChannelHandlerContext ctx = this.ctx;
        if (ctx != null) {
            ctx.close();
            this.ctx = null;
            broker().clientManager().clientDisconnected(this);
        }
    }

    @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
    public boolean writeRequest(JsonArray requests) {
        ChannelHandlerContext ctx = this.ctx;
        if (ctx == null) {
            return false;
        }
        if (!ctx.channel().isWritable()) {
            synchronized (this) {
                if (requestsCache == null) {
                    requestsCache = requests;
                } else {
                    requestsCache.mergeIn(requests);
                }
            }
            return true;
        }
        if (requestsCache != null) {
            synchronized (this) {
                if (requestsCache != null) {
                    requestsCache.mergeIn(requests);
                    requests = requestsCache;
                    requestsCache = null;
                }
            }
        }
        JsonObject top = new JsonObject();
        top.put("requests", requests);
        write(ctx, new String(top.encode(), CharsetUtil.UTF_8));
        return true;
    }

    @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
    public boolean writeResponse(JsonArray responses) {
        ChannelHandlerContext ctx = this.ctx;
        if (ctx == null) {
            return false;
        }
        if (!ctx.channel().isWritable()) {
            synchronized (this) {
                if (responsesCache == null) {
                    responsesCache = responses;
                } else {
                    responsesCache.mergeIn(responses);
                }
            }
            return true;
        }
        if (responsesCache != null) {
            synchronized (this) {
                if (responsesCache != null) {
                    responsesCache.mergeIn(responses);
                    responses = responsesCache;
                    responsesCache = null;
                }
            }
        }
        JsonObject top = new JsonObject();
        top.put("responses", responses);
        write(ctx, new String(top.encode(), CharsetUtil.UTF_8));
        return true;
    }

    private void write(ChannelHandlerContext ctx, String data) {
        byte[] bytes = data.getBytes(CharsetUtil.UTF_8);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        TextWebSocketFrame frame = new TextWebSocketFrame(buf);
        ctx.channel().writeAndFlush(frame);
        broker().metrics().incrementOut();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Sent] {}: {}", handshake().dsId(), data);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        write(ctx, "{}");
        broker.clientManager().clientConnected(this);
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
            broker().metrics().incrementIn();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[Received] {}: {}", handshake().dsId(), data);
            }
            if ("{}".equals(data)) {
                write(ctx, "{}");
            } else {
                try {
                    JsonObject obj = new JsonObject(data);
                    processor().processData(obj);
                } catch (RuntimeException e) {
                    String dsId = handshake().dsId();
                    String err = "Error occurred processing message for: {}\n{}";
                    LOGGER.error(err, dsId, e);
                }
            }
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
