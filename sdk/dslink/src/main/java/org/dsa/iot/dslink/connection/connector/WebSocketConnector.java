package org.dsa.iot.dslink.connection.connector;

import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.util.HttpClientUtils;
import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

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
    private long lastSentMessage;
    private WebSocket webSocket;

    public WebSocketConnector(DataHandler handler) {
        super(handler);
    }

    @Override
    public void start() {
        HttpClient client = HttpClientUtils.configure(getEndpoint());
        client.connectWebsocket(getUri(), new Handler<WebSocket>() {
            @Override
            public void handle(final WebSocket webSocket) {
                WebSocketConnector.this.webSocket = webSocket;
                setupPingHandler();

                Handler<Void> onConnected = getOnConnected();
                if (onConnected != null) {
                    onConnected.handle(null);
                }

                Handler<Throwable> onException = getOnException();
                if (onException != null) {
                    webSocket.exceptionHandler(onException);
                }

                final Handler<JsonObject> onData = getOnData();
                if (onData != null) {
                    webSocket.dataHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer event) {
                            String data = event.toString("UTF-8");
                            JsonObject obj = new JsonObject(data);
                            if (obj.containsField("ping")) {
                                String pong = data.replaceFirst("i", "o");
                                write(pong);
                                if (LOGGER.isDebugEnabled()) {
                                    String s = "Received ping, sending pong: {}";
                                    LOGGER.debug(s, pong);
                                }
                                return;
                            }
                            onData.handle(obj);
                        }
                    });
                }

                webSocket.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        Handler<Void> onDisconnected = getOnDisconnected();
                        if (onDisconnected != null) {
                            onDisconnected.handle(event);
                        }
                        close();
                    }
                });

            }
        });
    }

    @Override
    public void close() {
        if (webSocket != null) {
            try {
                webSocket.close();
            } catch (Exception ignored) {
            }

            webSocket = null;
        }

        if (pingHandler != null) {
            try {
                pingHandler.cancel(false);
            } catch (Exception ignored) {
            }
            pingHandler = null;
        }
    }

    @Override
    public void write(String data) {
        checkConnected();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sent data: {}", data);
        }
        webSocket.writeTextFrame(data);
        lastSentMessage = System.currentTimeMillis();
    }

    @Override
    public boolean isConnected() {
        return webSocket != null;
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
                        webSocket.writeTextFrame("{}");
                        LOGGER.debug("Sent ping");
                    } catch (Exception ignored) {
                    }
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void checkConnected() {
        if (webSocket == null) {
            throw new RuntimeException("Cannot write to unconnected connection");
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(WebSocketConnector.class);
    }
}
