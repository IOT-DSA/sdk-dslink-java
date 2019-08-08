package org.dsa.iot.dslink.connection;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.connector.WebSocketConnector;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.URLInfo;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class ConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private final Configuration configuration;
    private final LocalHandshake localHandshake;

    private Handler<Client> preInitHandler;
    private DataHandler handler;
    private NetworkClient client;
    private int delay = 1;

    private ScheduledFuture<?> future;
    private boolean running;

    public ConnectionManager(Configuration configuration,
                             LocalHandshake localHandshake) {
        this.configuration = configuration;
        this.localHandshake = localHandshake;
    }

    /**
     * The pre initialization handler allows the dslink to be configured
     * before the link is actually connected to the server.
     *
     * @param onClientInit Client initialization handler
     */
    public void setPreInitHandler(Handler<Client> onClientInit) {
        this.preInitHandler = onClientInit;
    }

    public synchronized void start(final Handler<Client> onClientConnected) {
        stop();
        running = true;

        LoopProvider.getProvider().schedule(new Runnable() {
            @Override
            public void run() {
                synchronized (ConnectionManager.this) {
                    if (!running) {
                        return;
                    }
                }
                LOGGER.debug("Initiating connection sequence");
                RemoteHandshake remoteHandshake = generateHandshake(new Handler<Exception>() {
                    @Override
                    public void handle(Exception event) {
                        LOGGER.error("Failed to complete handshake: {}", event.getMessage());
                        reconnect();
                    }
                });

                if (remoteHandshake == null) {
                    return;
                }

                if (handler == null) {
                    handler = new DataHandler();
                }

                boolean req = localHandshake.isRequester();
                boolean resp = localHandshake.isResponder();
                String path = remoteHandshake.getPath();
                final Client cc = new Client(req, resp, path);
                cc.setHandler(handler);

                if (preInitHandler != null) {
                    preInitHandler.handle(cc);
                }

                ConnectionType type = configuration.getConnectionType();
                switch (type) {
                    case WEB_SOCKET:
                        WebSocketConnector connector = new WebSocketConnector();
                        connector.setUseCompression(configuration.getConfig("ws_compression", true));
                        connector.setEndpoint(configuration.getAuthEndpoint());
                        connector.setRemoteHandshake(remoteHandshake);
                        connector.setLocalHandshake(localHandshake);
                        connector.setOnConnected(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                if (onClientConnected != null) {
                                    onClientConnected.handle(cc);
                                }
                                cc.connected();
                            }
                        });

                        connector.setOnDisconnected(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                cc.disconnected();
                                if (running) {
                                    LOGGER.warn("WebSocket connection failed");
                                    if (client != null) {
                                        client.close();
                                        client = null;
                                    }
                                    if (handler != null) {
                                        handler.onDisconnected();
                                    }
                                    reconnect();
                                }
                            }
                        });

                        connector.setOnData(new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject event) {
                                handler.processData(event);
                            }
                        });

                        client = connector;
                        handler.setClient(connector, remoteHandshake.getFormat());
                        connector.start();
                        break;
                    default:
                        throw new RuntimeException("Unhandled type: " + type);
                }
            }
        });
    }

    public synchronized void stop() {
        running = false;
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private RemoteHandshake generateHandshake(Handler<Exception> errorHandler) {
        try {
            synchronized (this) {
                if (!running) {
                    return null;
                }
            }
            URLInfo auth = configuration.getAuthEndpoint();
            return RemoteHandshake.generate(localHandshake, auth);
        } catch (Exception e) {
            if (errorHandler != null) {
                errorHandler.handle(e);
            }
        }
        return null;
    }

    private synchronized void reconnect() {
        if (!running) {
            return;
        }
        LOGGER.info("Reconnecting in {} seconds", delay);
        future = Objects.getDaemonThreadPool().schedule(new Runnable() {
            @Override
            public void run() {
                start(new Handler<Client>() {
                    @Override
                    public void handle(Client event) {
                        LOGGER.info("Connection established");
                        delay = 1;
                    }
                });
                delay *= 2;
                int cap = 60;
                if (delay > cap) {
                    delay = cap;
                }

                future = null;
            }
        }, delay, TimeUnit.SECONDS);
    }

    public static class Client {

        private final boolean isRequester;
        private final boolean isResponder;
        private final String path;

        private Handler<Client> onRequesterConnected;
        private Handler<Client> onResponderConnected;
        private Handler<Void> onRequesterDisconnected;
        private Handler<Void> onResponderDisconnected;
        private DataHandler handler;

        public Client(boolean isRequester,
                      boolean isResponder,
                      String path) {
            this.isRequester = isRequester;
            this.isResponder = isResponder;
            this.path = path;
        }

        public DataHandler getHandler() {
            return handler;
        }

        void setHandler(DataHandler handler) {
            this.handler = handler;
        }

        public boolean isRequester() {
            return isRequester;
        }

        public boolean isResponder() {
            return isResponder;
        }

        public String getPath() {
            return path;
        }

        public void setRequesterOnConnected(Handler<Client> handler) {
            this.onRequesterConnected = handler;
        }

        public void setResponderOnConnected(Handler<Client> handler) {
            this.onResponderConnected = handler;
        }

        public void setRequesterOnDisconnected(Handler<Void> handler) {
            this.onRequesterDisconnected = handler;
        }

        public void setResponderOnDisconnected(Handler<Void> handler) {
            this.onResponderDisconnected = handler;
        }

        void connected() {
            if (onRequesterConnected != null) {
                onRequesterConnected.handle(this);
            }

            if (onResponderConnected != null) {
                onResponderConnected.handle(this);
            }
        }

        void disconnected() {
            if (onRequesterDisconnected != null) {
                onRequesterDisconnected.handle(null);
            }

            if (onResponderDisconnected != null) {
                onResponderDisconnected.handle(null);
            }
        }
    }
}
