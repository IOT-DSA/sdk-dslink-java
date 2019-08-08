package org.dsa.iot.dslink.connection.connector;

import org.dsa.iot.dslink.connection.NetworkClient;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.provider.WsProvider;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.http.WsClient;
import org.dsa.iot.dslink.util.json.EncodingFormat;
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
    private NetworkClient writer;
    private long lastSentMessage;
    private boolean useCompression = true;

    @Override
    public void start() {
        URLInfo endpoint = getEndpoint();
        endpoint = new URLInfo(endpoint.protocol, endpoint.host,
                            endpoint.port, getUri(), endpoint.secure);
        WsProvider provider = WsProvider.getProvider();
        provider.setUseCompression(useCompression);
        provider.connect(new WsHandler(endpoint));
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
    public boolean writable() {
        return isConnected() && writer.writable();
    }

    @Override
    public void write(EncodingFormat format, JsonObject data) {
        checkConnected();

        writer.write(format, data);
        if (LOGGER.isDebugEnabled()) {
            String s = format.toJson();
            LOGGER.debug("Sent data ({}): {}", s, data);
        }
        lastSentMessage = System.currentTimeMillis();
    }

    @Override
    public boolean isConnected() {
        if (writer != null) {
            if (writer.isConnected()) {
                return true;
            }
            close();
            getOnDisconnected().handle(null);
        }
        return false;
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
                        write(getFormat(), new JsonObject());
                        LOGGER.debug("Sent ping");
                    } catch (Exception e) {
                        close();
                    }
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void setUseCompression(boolean use) {
        this.useCompression = use;
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
        public void onData(byte[] data, int offset, int length) {
            JsonObject obj = new JsonObject(getFormat(), data, offset, length);
            if (obj.contains("ping")) {
                obj.put("pong", obj.remove("ping"));
                WebSocketConnector.this.write(getFormat(), obj);
                if (LOGGER.isDebugEnabled()) {
                    String s = "Received ping, sending pong";
                    LOGGER.debug(s);
                }
                return;
            }
            Handler<JsonObject> h = getOnData();
            if (h != null) {
                h.handle(obj);
            }
        }

        @Override
        public void onConnected(NetworkClient writer) {
            WebSocketConnector.this.writer = writer;
            setupPingHandler();
            Handler<Void> onConnected = getOnConnected();
            if (onConnected != null) {
                onConnected.handle(null);
            }
        }

        @Override
        public void onDisconnected() {
            isConnected();
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
