package org.dsa.iot.dslink.connection;

import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.connector.WebSocketConnector;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.util.URLInfo;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class ConnectionManager {

    private final Configuration configuration;
    private final LocalHandshake localHandshake;

    private DataHandler handler;
    private NetworkClient client;

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

        RemoteHandshake currentHandshake = generateHandshake();
        int updateInterval = currentHandshake.getUpdateInterval();
        handler = new DataHandler(updateInterval);

        ConnectionType type = configuration.getConnectionType();
        switch (type) {
            case WEB_SOCKET:
                WebSocketConnector connector = new WebSocketConnector(handler);
                connector.setEndpoint(configuration.getAuthEndpoint());
                connector.setRemoteHandshake(currentHandshake);
                connector.setLocalHandshake(localHandshake);
                client = connector;
                handler.setClient(client);
                connector.start(new Handler<Void>() {
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
                break;
            default:
                throw new RuntimeException("Unhandled type: " + type);
        }
    }

    public void stop() {
        if (client != null) {
            client.close();
        }
    }

    private RemoteHandshake generateHandshake() {
        URLInfo auth = configuration.getAuthEndpoint();
        return RemoteHandshake.generate(localHandshake, auth);
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
