package org.dsa.iot.broker.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.client.ClientManager;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.List;

/**
 * @author Samuel Grenier
 */
public class WsServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final HttpVersion VERSION = HttpVersion.HTTP_1_1;

    private WebSocketServerHandshaker handshake;
    private final ClientManager manager;
    private final boolean secure;

    public WsServerHandler(ClientManager manager, boolean secure) {
        if (manager == null) {
            throw new NullPointerException("manager");
        }
        this.manager = manager;
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
        if (!req.decoderResult().isSuccess()) {
            HttpResponseStatus stat = HttpResponseStatus.BAD_REQUEST;
            FullHttpResponse resp = new DefaultFullHttpResponse(VERSION, stat);
            sendHttpResponse(ctx, resp);
            return;
        }

        QueryStringDecoder dec = new QueryStringDecoder(req.uri());
        String dsId = getParam(dec, "dsId");
        if ("/conn".equals(dec.path())
                && req.method() == HttpMethod.POST) {
            handleNewConn(ctx, req, dsId);
        } else if ("/ws".equals(dec.path())
                && req.method() == HttpMethod.GET) {
            String auth = getParam(dec, "auth");
            handleWsConn(ctx, req, auth, dsId);
        } else {
            sendForbidden(ctx);
        }
    }

    private void handleNewConn(ChannelHandlerContext ctx,
                               FullHttpRequest req,
                               String dsId) {
        ByteBuf content;
        {
            String data = req.content().toString(CharsetUtil.UTF_8);
            Client client = Client.create(dsId, new JsonObject(data));
            content = DsaHandshake.createHandshake(client);
            manager.clientConnecting(client);
        }
        HttpResponseStatus stat = HttpResponseStatus.OK;
        FullHttpResponse res = new DefaultFullHttpResponse(VERSION, stat, content);

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
    }

    private void handleWsConn(ChannelHandlerContext ctx,
                              FullHttpRequest req,
                              String auth,
                              String dsId) {
        // Validate the auth and dsId
        {
            Client client = manager.getPendingClient(dsId);
            if (client == null || !client.validate(auth)) {
                sendForbidden(ctx);
                return;
            }
        }

        // Allow the handshake to continue
        WebSocketServerHandshakerFactory ws = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, true, Integer.MAX_VALUE);
        handshake = ws.newHandshaker(req);
        if (handshake == null) {
            Channel c = ctx.channel();
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(c);
        } else {
            handshake.handshake(ctx.channel(), req);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx,
                                      WebSocketFrame frame) {
        final Channel channel = ctx.channel();
        if (frame instanceof TextWebSocketFrame) {
            String data = ((TextWebSocketFrame) frame).text();
            channel.writeAndFlush(data); // echo it out for now
        } else if (frame instanceof PingWebSocketFrame) {
            ByteBuf buf = frame.content().retain();
            channel.write(new PongWebSocketFrame(buf));
        } else if (frame instanceof CloseWebSocketFrame) {
            handshake.close(channel, (CloseWebSocketFrame) frame.retain());
        } else {
            String err = "%s frame types not supported";
            err = String.format(err, frame.getClass().getName());
            throw new UnsupportedOperationException(err);
        }
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

    private static void sendForbidden(ChannelHandlerContext ctx) {
        HttpResponseStatus stat = HttpResponseStatus.FORBIDDEN;
        sendHttpResponse(ctx, new DefaultFullHttpResponse(VERSION, stat));
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpResponse res) {
        Channel c = ctx.channel();
        c.writeAndFlush(res);
        c.close();
    }

    private static String getParam(QueryStringDecoder dec, String name) {
        List<String> list = dec.parameters().get(name);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }
}