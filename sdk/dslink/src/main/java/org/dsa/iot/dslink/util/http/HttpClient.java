package org.dsa.iot.dslink.util.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.dsa.iot.dslink.util.URLInfo;

/**
 * @author Samuel Grenier
 */
public class HttpClient {

    private final URLInfo url;

    public HttpClient(URLInfo url) {
        if (url == null) {
            throw new NullPointerException("url");
        }
        this.url = url;
    }

    public HttpResp post(String uri, String content) {
        if (uri == null) {
            throw new NullPointerException("uri");
        } else if (uri.isEmpty()) {
            uri = "/";
        }
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            final HttpHandler handler = new HttpHandler();

            Bootstrap b = new Bootstrap();
            b.group(group);
            b.channel(NioSocketChannel.class);
            b.handler(new Initializer(handler));
            ChannelFuture fut = b.connect(url.host, url.port);
            Channel chan = fut.sync().channel();
            chan.writeAndFlush(populateRequest(uri, content));
            fut.channel().closeFuture().sync();
            return populateResponse(handler);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            group.shutdownGracefully();
        }
    }

    private HttpRequest populateRequest(String uri, String content) {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, uri);
        ByteBuf buf = request.content();
        if (content != null) {
            byte[] bytes = content.getBytes(CharsetUtil.UTF_8);
            buf.writeBytes(bytes);
        }
        {
            HttpHeaders headers = request.headers();
            setHeaders(headers);
            setContentLength(headers, buf.readableBytes());
        }
        return request;
    }

    private HttpResp populateResponse(HttpHandler handler) throws Throwable {
        Throwable throwable = handler.getThrowable();
        if (throwable != null) {
            throw throwable;
        }

        HttpResp resp = new HttpResp();
        resp.setStatus(handler.getStatus());
        resp.setBody(handler.getContent());
        return resp;
    }

    private void setHeaders(HttpHeaders headers) {
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        headers.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.TEXT_PLAIN);
    }

    private void setContentLength(HttpHeaders headers, int length) {
        String len = String.valueOf(length);
        headers.set(HttpHeaderNames.CONTENT_LENGTH, len);
    }

    private static class Initializer extends ChannelInitializer<SocketChannel> {

        private final HttpHandler handler;

        public Initializer(HttpHandler handler) {
            this.handler = handler;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new HttpClientCodec());
            p.addLast(new HttpContentDecompressor());
            p.addLast(handler);
        }
    }

    private static class HttpHandler extends SimpleChannelInboundHandler<Object> {

        private StringBuffer content = new StringBuffer();
        private HttpResponseStatus status;
        private Throwable t;

        @Override
        protected void messageReceived(ChannelHandlerContext ctx,
                                       Object msg) throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse resp = (HttpResponse) msg;
                status = resp.status();
            }
            if (msg instanceof HttpContent) {
                ByteBuf buf = ((HttpContent) msg).content();
                content.append(buf.toString(CharsetUtil.UTF_8));
            }
            if (msg instanceof LastHttpContent) {
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
            this.t = t;
            ctx.close();
        }

        public Throwable getThrowable() {
            return t;
        }

        public HttpResponseStatus getStatus() {
            return status;
        }

        public String getContent() {
            return content.toString();
        }
    }
}
