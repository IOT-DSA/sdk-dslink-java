package org.dsa.iot.broker.server;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import org.dsa.iot.broker.client.ClientManager;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;

/**
 * @author Samuel Grenier
 */
public class ServerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);
    private final ClientManager manager;
    private final JsonObject serverConf;

    private EventLoopGroup bossLoop;
    private EventLoopGroup workerLoop;

    private Server httpServer;
    private Server httpsServer;

    public ServerManager(ClientManager manager,
                         JsonObject serverConf) {
        if (serverConf == null) {
            throw new NullPointerException("serverConf");
        } else if (manager == null) {
            throw new NullPointerException("manager");
        }
        this.manager = manager;
        this.serverConf = serverConf;
    }

    public void start() throws Exception {
        stop();
        LOGGER.info("Servers are starting");
        bossLoop = new NioEventLoopGroup(1);
        workerLoop = new NioEventLoopGroup();

        final JsonObject httpConf = serverConf.get("http");
        final JsonObject httpsConf = serverConf.get("https");

        final boolean http = httpConf.get("enabled");
        final boolean https = httpsConf.get("enabled");

        int httpPort = httpConf.get("port");
        int httpsPort = httpsConf.get("port");
        if ((http && https) && (httpPort == httpsPort)) {
            String err = "HTTP and HTTPS port are the same";
            throw new IllegalStateException(err);
        }

        if (http) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    startHttpServer(httpConf);
                }
            }, "Broker-HTTP-server");
            t.start();
        }

        if (https) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    startHttpsServer(httpsConf);
                }
            }, "Broker-HTTPS-server");
            t.start();
        }
    }

    public void stop() {
        if (!(bossLoop == null || workerLoop == null)) {
            bossLoop.shutdownGracefully();
            workerLoop.shutdownGracefully();
            if (httpServer != null) {
                httpServer.stop();
                httpServer = null;
            }
            if (httpsServer != null) {
                httpsServer.stop();
                httpsServer = null;
            }
        }
    }

    private void startHttpServer(JsonObject conf) {
        String host = conf.get("host");
        int port = conf.get("port");
        httpServer = new Server(host, port, null, manager);
        httpServer.start(bossLoop, workerLoop);
    }

    private void startHttpsServer(JsonObject conf) {
        String certChain = conf.get("certChainFile");
        if (certChain == null) {
            throw new RuntimeException("certChainFile not configured");
        }
        String certKey = conf.get("certKeyFile");
        if (certKey == null) {
            throw new RuntimeException("certChainKey not configured");
        }
        String certKeyPass = conf.get("certKeyPass");

        File cc = new File(certChain);
        File ck = new File(certKey);
        SslContext ssl;
        try {
            ssl = SslContext.newServerContext(cc, ck, certKeyPass);
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }

        String host = conf.get("host");
        int port = conf.get("port");
        httpsServer = new Server(host, port, ssl, manager);
        httpsServer.start(bossLoop, workerLoop);
    }
}
