package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.connection.RemoteEndpoint;
import org.dsa.iot.dslink.connection.connector.WebSocketConnector;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.handshake.RemoteHandshake;
import org.dsa.iot.dslink.util.Arguments;
import org.dsa.iot.dslink.util.Configuration;
import org.dsa.iot.dslink.util.LogLevel;
import org.dsa.iot.dslink.util.URLInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for generating {@link DSLink} objects.
 *
 * @author Samuel Grenier
 */
public class DSLinkFactory {

    /**
     * Creates a DSLink provider. The provider will automatically connect
     * and block.
     *
     * @param name Name of the link, prepended in the DsId.
     * @param args Arguments to parse
     * @param handler DSLink handler
     */
    public static void startRequester(String name,
                                        String[] args,
                                        DSLinkHandler handler) {
        DSLinkProvider provider = generateRequester(name, args, handler);
        if (provider != null) {
            provider.start();
            provider.sleep();
        }
    }

    /**
     * Creates a DSLink provider. The provider will automatically connect
     * and block.
     *
     * @param name Name of the link, prepended in the DsId.
     * @param args Arguments to parse
     * @param handler DSLink handler
     */
    public static void startResponder(String name,
                                      String[] args,
                                      DSLinkHandler handler) {
        DSLinkProvider provider = generateResponder(name, args, handler);
        if (provider != null) {
            provider.start();
            provider.sleep();
        }
    }

    /**
     * Creates a DSLink provider based on the arguments passed into main. The
     * link handler does not need to set the keys or the authentication
     * endpoint.
     *
     * @param name Name of the link, prepended in the DsId.
     * @param args Arguments to parse
     * @param handler DSLink handler
     * @return A link provider or null if the args have a help parameter
     *         passed in.
     */
    @SuppressWarnings("ConstantConditions")
    public static DSLinkProvider generateResponder(String name,
                                                   String[] args,
                                                   DSLinkHandler handler) {
        return generate(name, args, handler, false, true);
    }

    /**
     * Creates a DSLink provider based on the arguments passed into main. The
     * link handler does not need to set the keys or the authentication
     * endpoint.
     *
     * @param name Name of the link, prepended in the DsId.
     * @param args Arguments to parse
     * @param handler DSLink handler
     * @return A link provider or null if the args have a help parameter
     *         passed in.
     */
    @SuppressWarnings("ConstantConditions")
    public static DSLinkProvider generateRequester(String name,
                                                    String[] args,
                                                    DSLinkHandler handler) {
        return generate(name, args, handler, true, false);
    }

    /**
     * Creates a DSLink provider based on the arguments passed into main. The
     * link handler does not need to set the keys or the authentication
     * endpoint.
     *
     * @param name Name of the link, prepended in the DsId.
     * @param args Arguments to parse
     * @param handler DSLink handler
     * @return A link provider or null if the args have a help parameter
     *         passed in.
     */
    @SuppressWarnings("ConstantConditions")
    public static DSLinkProvider generate(String name,
                                            String[] args,
                                            DSLinkHandler handler,
                                            boolean requester,
                                            boolean responder) {
        Configuration defaults = new Configuration();
        defaults.setDsId(name);
        defaults.setConnectionType(ConnectionType.WEB_SOCKET);
        defaults.setRequester(requester);
        defaults.setResponder(responder);

        Arguments parsed = Arguments.parse(args);
        if (parsed == null) {
            return null;
        }

        LogLevel.setLevel(parsed.getLogLevel());
        defaults.setAuthEndpoint(parsed.getBrokerHost());

        Path loc = Paths.get(parsed.getKeyPath());
        defaults.setKeys(LocalKeys.getFromFileSystem(loc));

        loc = Paths.get(parsed.getNodesPath());
        defaults.setSerializationPath(loc);

        handler.setConfig(defaults);
        return generate(handler);
    }

    /**
     * Generates a DSLink that provides a fully managed state automatically
     * based on the configuration.
     *
     * @param handler DSLink handler
     * @return A DSLink provider that handles when a connection is successful
     *         or a client connected to the server.
     */
    public static DSLinkProvider generate(DSLinkHandler handler) {
        Configuration config;
        if (handler == null)
            throw new NullPointerException("handler");
        else if ((config = handler.getConfig()) == null)
            throw new NullPointerException("handler configuration not set");

        LocalKeys keys = config.getKeys();
        if (keys == null) {
            keys = LocalKeys.generate();
            config.setKeys(keys);
        }
        config.validate();

        URLInfo endpoint = config.getAuthEndpoint();
        LocalHandshake lh = new LocalHandshake(config);

        ConnectionType type = config.getConnectionType();
        RemoteEndpoint rep;
        switch (type) {
            case WEB_SOCKET:
                RemoteHandshake rh = RemoteHandshake.generate(lh, endpoint);
                rep = new WebSocketConnector();
                rep.setEndpoint(endpoint);
                rep.setLocalHandshake(lh);
                rep.setRemoteHandshake(rh);
                break;
            default:
                throw new RuntimeException("Unhandled connection type: " + type.name());
        }

        DSLinkProvider provider = new DSLinkProvider(rep, handler);
        provider.setDefaultEndpointHandler();
        return provider;
    }
}
