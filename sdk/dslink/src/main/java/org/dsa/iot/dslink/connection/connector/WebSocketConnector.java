package org.dsa.iot.dslink.connection.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles connecting to web socket servers.
 *
 * @author Samuel Grenier
 */
public class WebSocketConnector extends RemoteEndpoint {

    private static final Logger LOGGER;

    private EventLoopGroup eventLoopGroup;
    private ScheduledFuture<?> pingHandler;
    private long lastSentMessage;
    private Channel channel;

    @Override
    public void start() {
        eventLoopGroup = new NioEventLoopGroup();
        try {
            final URLInfo endpoint = getEndpoint();
            String full = endpoint.protocol + "://" + endpoint.host
                    + ":" + endpoint.port + getUri();
            URI uri = new URI(full);
            WebSocketVersion v = WebSocketVersion.V13;
            HttpHeaders h = new DefaultHttpHeaders();
            final WebSocketClientHandshaker wsch = WebSocketClientHandshakerFactory
                    .newHandshaker(uri, v, null, true, h, Integer.MAX_VALUE);
            final WebSocketHandler handler = new WebSocketHandler(wsch);

            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup);
            b.channel(NioSocketChannel.class);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    if (endpoint.secure) {
                        TrustManagerFactory man = InsecureTrustManagerFactory.INSTANCE;
                        SslContext con = SslContext.newClientContext(man);
                        p.addLast(con.newHandler(ch.alloc()));
                    }

                    p.addLast(new HttpClientCodec());
                    p.addLast(new HttpObjectAggregator(8192));
                    p.addLast(new WebSocketClientCompressionHandler());
                    p.addLast(handler);
                }
            });

            channel = b.connect(endpoint.host, endpoint.port).sync().channel();
            handler.handshakeFuture().sync().channel();
            Handler<Void> onConnected = getOnConnected();
            if (onConnected != null) {
                onConnected.handle(null);
            }
        } catch (Exception e) {
            eventLoopGroup.shutdownGracefully();
        }
    }

    @Override
    public void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception ignored) {
            }
            channel = null;
        }
        if (pingHandler != null) {
            try {
                pingHandler.cancel(false);
            } catch (Exception ignored) {
            }
            pingHandler = null;
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
            eventLoopGroup = null;
        }
    }

    @Override
    public void write(String data) {
        checkConnected();

        byte[] bytes;
        try {
            bytes = data.getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        WebSocketFrame frame = new TextWebSocketFrame(buf);
        channel.writeAndFlush(frame);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sent data: {}", data);
        }

        lastSentMessage = System.currentTimeMillis();
    }

    @Override
    public boolean isConnected() {
        return channel != null;
    }

    private void setupPingHandler() {
        if (pingHandler != null) {
            pingHandler.cancel(false);
        }

        pingHandler = Objects.getDaemonThreadPool().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastSentMessage >= 29000) {
                    try {
                        write("{}");
                        LOGGER.debug("Sent ping");
                    } catch (Exception e) {
                        close();
                    }
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void checkConnected() {
        if (!isConnected()) {
            throw new RuntimeException("Cannot write to unconnected connection");
        }
    }

    private class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshake;
        private ChannelPromise handshakeFuture;

        public WebSocketHandler(WebSocketClientHandshaker handshake) {
            this.handshake = handshake;
        }

        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            super.handlerAdded(ctx);
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            handshake.handshake(ctx.channel());
            setupPingHandler();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            Handler<Void> onDisconnected = getOnDisconnected();
            if (onDisconnected != null) {
                onDisconnected.handle(null);
            }
            WebSocketConnector.this.close();
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, Object msg) {
            Channel ch = ctx.channel();
            if (!handshake.isHandshakeComplete()) {
                handshake.finishHandshake(ch, (FullHttpResponse) msg);
                handshakeFuture.setSuccess();
                return;
            }

            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                throw new IllegalStateException(
                        "Unexpected FullHttpResponse (getStatus=" + response.status() +
                                ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
            }

            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                {
                    String data = textFrame.text();
                    JsonObject obj = new JsonObject(textFrame.text());
                    if (obj.contains("ping")) {
                        String pong = data.replaceFirst("i", "o");
                        WebSocketConnector.this.write(pong);
                        if (LOGGER.isDebugEnabled()) {
                            String s = "Received ping, sending pong: {}";
                            LOGGER.debug(s, pong);
                        }
                        return;
                    }
                    Handler<JsonObject> h = getOnData();
                    if (h != null) {
                        h.handle(obj);
                    }
                }
            } else if (frame instanceof CloseWebSocketFrame) {
                Handler<Void> onDisconnected = getOnDisconnected();
                if (onDisconnected != null) {
                    onDisconnected.handle(null);
                }
                WebSocketConnector.this.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            ctx.close();
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(WebSocketConnector.class);
    }
}
