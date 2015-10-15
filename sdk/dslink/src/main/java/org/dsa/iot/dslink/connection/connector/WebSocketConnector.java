package org.dsa.iot.dslink.connection.connector;

import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.provider.WsProvider;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.http.WsClient;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles connecting to web socket servers.
 *
 * @author Samuel Grenier
 */
public class WebSocketConnector extends RemoteEndpoint {

    private static final Logger LOGGER;

    private ScheduledFuture<?> pingHandler;
    private WsProvider.Writer writer;
    private long lastSentMessage;

    @Override
    public void start() {
        URLInfo endpoint = getEndpoint();
        endpoint = new URLInfo(endpoint.protocol, endpoint.host,
                            endpoint.port, getUri(), endpoint.secure);
        WsProvider.getProvider().connect(new WsHandler(endpoint));
    }

    @Override
    public void close() {
        if (pingHandler != null) {
            try {
                pingHandler.cancel(false);
            } catch (Exception ignored) {
            }
            pingHandler = null;
        }
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception ignored) {
            }
            writer = null;
        }
    }

    @Override
    public void write(String data) {
        checkConnected();

        writer.write(data);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sent data: {}", data);
        }
        lastSentMessage = System.currentTimeMillis();
    }

    @Override
    public boolean isConnected() {
        return writer != null;
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

    private class WsHandler extends WsClient {

        public WsHandler(URLInfo url) {
            super(url);
        }

        @Override
        public void onData(String data) {
            JsonObject obj = new JsonObject(data);
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

        @Override
        public void onConnected(WsProvider.Writer writer) {
            WebSocketConnector.this.writer = writer;
            setupPingHandler();
            Handler<Void> onConnected = getOnConnected();
            if (onConnected != null) {
                onConnected.handle(null);
            }
        }

        @Override
        public void onDisconnected() {
            if (!isConnected()) {
                return;
            }
            WebSocketConnector.this.close();
            Handler<Void> onDisconnected = getOnDisconnected();
            if (onDisconnected != null) {
                onDisconnected.handle(null);
            }
        }

        @Override
        public void onThrowable(Throwable throwable) {
            LOGGER.error("", throwable);
            onDisconnected();
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(WebSocketConnector.class);
    }
}
