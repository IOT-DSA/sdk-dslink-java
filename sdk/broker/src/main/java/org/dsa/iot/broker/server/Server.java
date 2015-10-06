package org.dsa.iot.broker.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private final String host;
    private final int port;
    private final SslContext ssl;
    private Channel channel;

    public Server(String host, int port, SslContext ssl) {
        if (host == null) {
            throw new NullPointerException("host");
        } else if (port < 0) {
            throw new IllegalArgumentException("port");
        }
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    public void start(EventLoopGroup bossLoop,
                      EventLoopGroup workerLoop) {
        ServerBootstrap strap = new ServerBootstrap();
        strap.option(ChannelOption.SO_REUSEADDR, false);
        strap.channel(NioServerSocketChannel.class);
        strap.childHandler(new WsServerInitializer());
        strap.group(bossLoop, workerLoop);

        ChannelFuture fut = strap.bind(host, port);
        fut.syncUninterruptibly();
        {
            String pretty = ssl != null ? "HTTPS" : "HTTP";
            LOGGER.info("{} server bound to {} on port {}", pretty, host, port);
        }
        channel = fut.channel();
        channel.closeFuture().syncUninterruptibly();
    }

    public void stop() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    private class WsServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (ssl != null) {
                pipeline.addLast(ssl.newHandler(ch.alloc()));
            }
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(65536));
            pipeline.addLast(new WebSocketServerCompressionHandler());
            pipeline.addLast(new WsServerHandler(ssl != null));
        }
    }
}
