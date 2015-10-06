package org.dsa.iot.broker;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import org.dsa.iot.broker.config.Arguments;
import org.dsa.iot.broker.config.broker.BrokerConfig;
import org.dsa.iot.broker.config.broker.BrokerFileConfig;
import org.dsa.iot.broker.config.broker.BrokerMemoryConfig;
import org.dsa.iot.broker.server.Server;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;

/**
 * @author Samuel Grenier
 */
public class Broker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Broker.class);
    private final BrokerConfig config;
    private EventLoopGroup bossLoop;
    private EventLoopGroup workerLoop;

    private Server httpServer;
    private Server httpsServer;

    public Broker(BrokerConfig config) {
        if (config == null) {
            throw new NullPointerException("config");
        }
        this.config = config;
        config.readAndUpdate();
    }

    protected void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }, "Broker-Shutdown-Hook"));
    }

    /**
     * Starts the broker.
     *
     * @see BrokerMemoryConfig For the default configurations.
     */
    public void start() {
        stop();
        LOGGER.info("Broker is starting");
        bossLoop = new NioEventLoopGroup(1);
        workerLoop = new NioEventLoopGroup();

        final JsonObject conf = config.getConfig().get("server");
        final JsonObject httpConf = conf.get("http");
        final JsonObject httpsConf = conf.get("https");
        boolean http = httpConf.get("enabled");
        boolean https = httpsConf.get("enabled");

        int httpPort = httpConf.get("port");
        int httpsPort = httpsConf.get("port");
        if ((http && https) && (httpPort == httpsPort)) {
            String err = "HTTP and HTTPS port are the same";
            throw new IllegalStateException(err);
        }

        try {
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
        } catch (Exception e) {
            stop();
            throw new RuntimeException(e);
        }
    }

    /**
     * Shuts down the broker.
     */
    public void stop() {
        if (!(bossLoop == null || workerLoop == null)) {
            LOGGER.info("Broker is shutting down");
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
        httpServer = new Server(host, port, null);
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
        httpsServer = new Server(host, port, ssl);
        httpsServer.start(bossLoop, workerLoop);
    }

    /**
     * Creates a standard broker with unmodified functionality.
     *
     * @param args Arguments of the program.
     * @return A broker instance.
     */
    public static Broker create(String[] args) {
        Arguments parsed = new Arguments();
        if (!parsed.parse(args)) {
            return null;
        }

        if (!parsed.runServer()) {
            parsed.displayHelp();
            return null;
        }

        BrokerConfig conf = new BrokerFileConfig();
        Broker b = new Broker(conf);
        b.addShutdownHook();
        return b;
    }
}
