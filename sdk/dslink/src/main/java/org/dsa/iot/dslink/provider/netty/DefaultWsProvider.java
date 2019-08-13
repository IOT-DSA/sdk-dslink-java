package org.dsa.iot.dslink.provider.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateClientExtensionHandshaker;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.provider.WsProvider;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.http.WsClient;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.shared.SharedObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Samuel Grenier
 */
public class DefaultWsProvider extends WsProvider {

    private static final Logger LOGGER;

    @Override
    public void connect(WsClient client) {
        if (client == null) {
            throw new NullPointerException("client");
        }
        final URLInfo url = client.getUrl();
        String full = url.protocol + "://" + url.host
                + ":" + url.port + url.path;
        full = full.replaceAll("%", "%25");
        URI uri;
        try {
            uri = new URI(full);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        WebSocketVersion v = WebSocketVersion.V13;
        HttpHeaders h = new DefaultHttpHeaders();
        final WebSocketClientHandshaker wsch = WebSocketClientHandshakerFactory
                .newHandshaker(uri, v, null, true, h, Integer.MAX_VALUE);
        final WebSocketHandler handler = new WebSocketHandler(wsch, client);

        Bootstrap b = new Bootstrap();
        b.group(SharedObjects.getLoop());
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                if (url.secure) {
                    TrustManagerFactory man = InsecureTrustManagerFactory.INSTANCE;
                    SslContextBuilder scb = SslContextBuilder.forClient();
                    SslContext con = scb.trustManager(man).build();
                    p.addLast(con.newHandler(ch.alloc()));
                }

                p.addLast(new HttpClientCodec());
                p.addLast(new HttpObjectAggregator(8192));
                WebSocketClientExtensionHandshaker com
                        = new PerMessageDeflateClientExtensionHandshaker();
                if (getUseCompression()) {
                    p.addLast(new WebSocketClientExtensionHandler(com));
                }
                p.addLast(handler);
            }
        });
        b.connect(url.host, url.port);
    }

    private static class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

        private final WsClient client;

        private WebSocketClientHandshaker handshake;

        public WebSocketHandler(WebSocketClientHandshaker handshake,
                                WsClient client) {
            this.handshake = handshake;
            this.client = client;
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            handshake.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            client.onDisconnected();
        }

        @Override
        public void channelRead0(final ChannelHandlerContext ctx,
                                 Object msg) {
            final Channel ch = ctx.channel();
            if (handshake != null && !handshake.isHandshakeComplete()) {
                try {
                    handshake.finishHandshake(ch, (FullHttpResponse) msg);
                } catch (Throwable throwable) {
                    client.onThrowable(throwable);
                    throw throwable;
                }

                handshake = null;
                client.onConnected(new NetworkClient() {

                    @Override
                    public boolean writable() {
                        return ch.isWritable();
                    }

                    @Override
                    public void write(EncodingFormat format,
                                      JsonObject data) {
                        byte[] bytes = data.encode(format);
                        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
                        WebSocketFrame frame = null;
                        if (format == EncodingFormat.JSON) {
                            frame = new TextWebSocketFrame(buf);
                        } else {
                            String err = "Unsupported encoding format: {}";
                            LOGGER.error(err, format);
                        }
                        if (frame != null) {
                            ch.writeAndFlush(frame);
                        }
                    }

                    @Override
                    public void close() {
                        ctx.close();
                    }

                    @Override
                    public boolean isConnected() {
                        return ch.isOpen();
                    }
                });
                return;
            }

            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                throw new IllegalStateException(
                        "Unexpected FullHttpResponse (getStatus=" + response.status() +
                                ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
            }

            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame
                    || frame instanceof BinaryWebSocketFrame) {
                ByteBuf content = frame.content();
                int offset = 0;
                int length = content.readableBytes();
                byte[] bytes;
                {
                    if (content.hasArray()) {
                        offset = content.arrayOffset();
                        bytes = content.array();
                    } else {
                        bytes = new byte[length];
                        content.readBytes(bytes);
                    }
                }
                client.onData(bytes, offset, length);
            } else if (frame instanceof PingWebSocketFrame) {
                ByteBuf buf = frame.content().retain();
                PongWebSocketFrame pong = new PongWebSocketFrame(buf);
                ctx.channel().writeAndFlush(pong);
            } else if (frame instanceof CloseWebSocketFrame) {
                client.onDisconnected();
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            client.onThrowable(cause);
            ctx.close();
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(DefaultWsProvider.class);
    }
}
