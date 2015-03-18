package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.connection.connector.WebSocketConnector;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.util.Configuration;
import org.dsa.iot.dslink.util.URLInfo;

/**
 * Factory for generating {@link DSLink} objects.
 *
 * @author Samuel Grenier
 */
public class DSLinkFactory {

    /**
     * Generates a DSLink that provides a fully managed state automatically
     * based on the configuration.
     *
     * @param handler DSLink handler
     * @return A DSLink that can manage the connection state
     */
    public static DSLink generate(DSLinkHandler handler) {
        Configuration config;
        if (handler == null)
            throw new NullPointerException("handler");
        else if ((config = handler.getConfig()) == null)
            throw new NullPointerException("handler configuration");

        LocalKeys keys = config.getKeys();
        if (keys == null) {
            keys = LocalKeys.generate();
            config.setKeys(keys);
        }
        config.validate();

        final NodeManager manager = new NodeManager();
        URLInfo endpoint = config.getAuthEndpoint();
        LocalHandshake lh = new LocalHandshake(config);

        ConnectionType type = config.getConnectionType();
        RemoteEndpoint rep;
        switch (type) {
            case WEB_SOCKET:
                RemoteHandshake rh = RemoteHandshake.generate(lh, endpoint);
                rep = new WebSocketConnector();
                rep.setRequester(new Requester(handler));
                rep.setEndpoint(endpoint);
                rep.setLocalHandshake(lh);
                rep.setRemoteHandshake(rh);
                break;
            default:
                throw new RuntimeException("Unhandled connection type: " + type.name());
        }

        return new DSLink(rep, manager, handler);
    }
}
