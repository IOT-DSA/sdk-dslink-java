package org.dsa.iot.dslink.provider.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import org.dsa.iot.dslink.provider.HttpProvider;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.http.HttpResp;
import org.dsa.iot.shared.SharedObjects;

import javax.net.ssl.TrustManagerFactory;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class DefaultHttpProvider extends HttpProvider {

    @Override
    public HttpResp post(URLInfo url,
                         String content,
                         Map<String, String> headers) {
        if (url == null) {
            throw new NullPointerException("url");
        }

        try {
            final HttpHandler handler = new HttpHandler();

            Bootstrap b = new Bootstrap();
            b.group(SharedObjects.getLoop());
            b.channel(NioSocketChannel.class);
            b.handler(new Initializer(handler, url.secure));
            ChannelFuture fut = b.connect(url.host, url.port);
            Channel chan = fut.sync().channel();
            chan.writeAndFlush(populateRequest(url.path, content, headers));
            fut.channel().closeFuture().sync();
            return populateResponse(handler);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private HttpRequest populateRequest(String uri,
                                        String content,
                                        Map<String, String> headers) {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, uri);
        ByteBuf buf = request.content();
        if (content != null) {
            byte[] bytes = content.getBytes(CharsetUtil.UTF_8);
            buf.writeBytes(bytes);
        }
        {
            HttpHeaders h = request.headers();
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    h.set(name, value);
                }
            }
            String len = String.valueOf(buf.readableBytes());
            h.set(HttpHeaderNames.CONTENT_LENGTH, len);
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

    private static class Initializer extends ChannelInitializer<SocketChannel> {

        private final HttpHandler handler;
        private final boolean secure;

        public Initializer(HttpHandler handler, boolean secure) {
            this.handler = handler;
            this.secure = secure;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();

            if (secure) {
                TrustManagerFactory man = InsecureTrustManagerFactory.INSTANCE;
                SslContext con = SslContext.newClientContext(man);
                p.addLast(con.newHandler(ch.alloc()));
            }

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
