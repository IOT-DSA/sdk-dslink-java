package org.dsa.iot.dslink;

import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.ConnectionManager;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.util.log.LogLevel;
import org.dsa.iot.dslink.util.log.LogManager;

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
     * @param args    Arguments to parse
     * @param handler DSLink handler
     */
    public static void start(String[] args, DSLinkHandler handler) {
        startProvider(generate(args, handler));
    }

    /**
     * Creates a DSLink provider based on the arguments passed into main. The
     * link handler does not need to set the keys or the authentication
     * endpoint.
     *
     * @param args      Arguments to parse.
     * @param handler   DSLink handler.
     * @return A link provider or null if the args have a help parameter
     * passed in.
     */
    public static DSLinkProvider generate(String[] args,
                                          DSLinkHandler handler) {
        // Log level is set until the configuration is read and sets
        // log level to the designated level.
        LogManager.setLevel(LogLevel.ERROR);

        boolean req = handler.isRequester();
        boolean resp = handler.isResponder();
        handler.setConfig(Configuration.autoConfigure(args, req, resp));
        return generate(handler);
    }

    /**
     * Generates a DSLink that provides a fully managed state automatically
     * based on the configuration.
     *
     * @param handler DSLink handler
     * @return A DSLink provider that handles when a connection is successful
     * or a client connected to the server.
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

        LocalHandshake lh = new LocalHandshake(config);
        ConnectionManager manager = new ConnectionManager(config, lh);
        return new DSLinkProvider(manager, handler);
    }

    private static void startProvider(DSLinkProvider provider) {
        if (provider != null) {
            provider.start();
            provider.sleep();
        }
    }
}
