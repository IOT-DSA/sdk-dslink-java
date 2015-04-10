package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.connector.WebSocketConnector;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.URLInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Samuel Grenier
 */
public class ConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private final Configuration configuration;
    private final LocalHandshake localHandshake;

    private DataHandler handler;
    private NetworkClient client;
    private int delay = 1;

    public ConnectionManager(Configuration configuration,
                             LocalHandshake localHandshake) {
        this.configuration = configuration;
        this.localHandshake = localHandshake;
    }

    public DataHandler getHandler() {
        return handler;
    }

    public void start(final Handler<ClientConnected> onClientConnected) {
        stop();

        final ScheduledThreadPoolExecutor stpe = Objects.getThreadPool();
        stpe.execute(new Runnable() {
            @Override
            public void run() {
                RemoteHandshake currentHandshake = generateHandshake(new Handler<Exception>() {
                    @Override
                    public void handle(Exception event) {
                        LOGGER.error("Failed to complete handshake: {}", event.getMessage());
                        reconnect();
                    }
                });

                if (currentHandshake == null) {
                    return;
                }

                int updateInterval = currentHandshake.getUpdateInterval();
                if (handler == null) {
                    handler = new DataHandler(updateInterval);
                }

                ConnectionType type = configuration.getConnectionType();
                switch (type) {
                    case WEB_SOCKET:
                        WebSocketConnector connector = new WebSocketConnector(handler);
                        connector.setEndpoint(configuration.getAuthEndpoint());
                        connector.setRemoteHandshake(currentHandshake);
                        connector.setLocalHandshake(localHandshake);
                        connector.setOnConnected(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                if (onClientConnected != null) {
                                    boolean req = localHandshake.isRequester();
                                    boolean resp = localHandshake.isResponder();
                                    NetworkClient c = client;
                                    DataHandler h = handler;
                                    ClientConnected cc = new ClientConnected(c, h, req, resp);
                                    onClientConnected.handle(cc);
                                }
                            }
                        });

                        connector.setOnDisconnected(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                LOGGER.warn("WebSocket connection failed");
                                reconnect();
                            }
                        });

                        connector.setOnException(new Handler<Throwable>() {
                            @Override
                            public void handle(Throwable event) {
                                event.printStackTrace();
                            }
                        });

                        connector.setOnData(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer event) {
                                getHandler().processData(event);
                            }
                        });

                        client = connector;
                        handler.setClient(client);
                        connector.start();
                        break;
                    default:
                        throw new RuntimeException("Unhandled type: " + type);
                }
            }
        });
    }

    public void stop() {
        if (client != null) {
            client.close();
        }
    }

    private RemoteHandshake generateHandshake(Handler<Exception> errorHandler) {
        try {
            URLInfo auth = configuration.getAuthEndpoint();
            return RemoteHandshake.generate(localHandshake, auth);
        } catch (Exception e) {
            if (errorHandler != null) {
                errorHandler.handle(e);
            }
        }
        return null;
    }

    private void reconnect() {
        LOGGER.info("Reconnecting in {} seconds", delay);
        Objects.getThreadPool().schedule(new Runnable() {
            @Override
            public void run() {
                start(new Handler<ClientConnected>() {
                    @Override
                    public void handle(ClientConnected event) {
                        LOGGER.info("Connection established");
                        delay = 1;
                    }
                });
                delay *= 2;
                int cap = 120;
                if (delay > cap) {
                    delay = cap;
                }
            }
        }, delay, TimeUnit.SECONDS);
    }

    public static class ClientConnected {

        private final NetworkClient client;
        private final DataHandler handler;
        private final boolean isRequester;
        private final boolean isResponder;

        public ClientConnected(NetworkClient client,
                               DataHandler handler,
                               boolean isRequester,
                               boolean isResponder) {
            this.client = client;
            this.handler = handler;
            this.isRequester = isRequester;
            this.isResponder = isResponder;
        }

        public NetworkClient getClient() {
            return client;
        }

        public DataHandler getHandler() {
            return handler;
        }

        public boolean isRequester() {
            return isRequester;
        }

        public boolean isResponder() {
            return isResponder;
        }
    }
}
