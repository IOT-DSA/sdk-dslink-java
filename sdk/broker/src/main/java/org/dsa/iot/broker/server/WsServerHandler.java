package org.dsa.iot.broker.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

/**
 * @author Samuel Grenier
 */
public class WsServerHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;
    private final boolean secure;

    public WsServerHandler(boolean secure) {
        this.secure = secure;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx,
                                   FullHttpRequest req) {
        final HttpVersion v = HttpVersion.HTTP_1_1;
        if (!req.decoderResult().isSuccess()) {
            HttpResponseStatus stat = HttpResponseStatus.BAD_REQUEST;
            FullHttpResponse resp = new DefaultFullHttpResponse(v, stat);
            sendHttpResponse(ctx, resp);
            return;
        }

        QueryStringDecoder dec = new QueryStringDecoder(req.uri());
        if ("/conn".equals(dec.path())
                && req.method() == HttpMethod.POST) {
            ByteBuf content = DsaHandshaker.createHandshake();
            HttpResponseStatus stat = HttpResponseStatus.OK;
            FullHttpResponse res = new DefaultFullHttpResponse(v, stat, content);

            HttpHeaders h = res.headers();
            {
                AsciiString s = HttpHeaderNames.CONTENT_TYPE;
                h.set(s, "text/json; charset=UTF-8");
            }
            {
                AsciiString s = HttpHeaderNames.CONTENT_LENGTH;
                String len = String.valueOf(content.readableBytes());
                h.set(s, len);
            }

            sendHttpResponse(ctx, res);
        } else if ("/ws".equals(dec.path())
                && req.method() == HttpMethod.GET) {
            WebSocketServerHandshakerFactory ws = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(req), null, true, Integer.MAX_VALUE);
            handshaker = ws.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
            }
        } else {
            HttpResponseStatus stat = HttpResponseStatus.FORBIDDEN;
            sendHttpResponse(ctx, new DefaultFullHttpResponse(v, stat));
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }

        // Send the uppercase string back.
        String request = ((TextWebSocketFrame) frame).text();
        System.err.printf("%s received %s%n", ctx.channel(), request);
        ctx.channel().write(new TextWebSocketFrame(request.toUpperCase()));
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpResponse res) {
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();

            String len = String.valueOf(res.content().readableBytes());
            res.headers().set(HttpHeaderNames.CONTENT_LENGTH, len);
        }

        ChannelFuture f = ctx.channel().writeAndFlush(res);
        f.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private String getWebSocketLocation(FullHttpRequest req) {
        String host = req.headers().get(HttpHeaderNames.HOST).toString();
        String location = host + "/ws";
        if (secure) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }
}