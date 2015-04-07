package org.dsa.iot.dslink;

import ch.qos.logback.classic.Level;
import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.ConnectionManager;
import org.dsa.iot.dslink.handshake.LocalHandshake;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.util.LogLevel;

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
     * @param name    Name of the link, prepended in the DsId.
     * @param args    Arguments to parse
     * @param handler DSLink handler
     */
    public static void startRequester(String name,
                                      String[] args,
                                      DSLinkHandler handler) {
        startProvider(generateRequester(name, args, handler));
    }

    /**
     * Creates a DSLink provider. The provider will automatically connect
     * and block.
     *
     * @param name    Name of the link, prepended in the DsId.
     * @param args    Arguments to parse
     * @param handler DSLink handler
     */
    public static void startResponder(String name,
                                      String[] args,
                                      DSLinkHandler handler) {
        startProvider(generateResponder(name, args, handler));
    }

    /**
     * Starts a link that is both a requester and a responder. The provider
     * is then created, starts a connection, and blocks.
     *
     * @param name      Name of the link
     * @param args      Arguments to parse
     * @param handler   DSLink handler
     */
    public static void startDual(String name,
                                 String[] args,
                                 DSLinkHandler handler) {
        startProvider(generate(name, args, handler, true, true));
    }

    /**
     * Creates a DSLink provider based on the arguments passed into main. The
     * link handler does not need to set the keys or the authentication
     * endpoint.
     *
     * @param name    Name of the link, prepended in the DsId.
     * @param args    Arguments to parse
     * @param handler DSLink handler
     * @return A link provider or null if the args have a help parameter
     * passed in.
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
     * @param name    Name of the link, prepended in the DsId.
     * @param args    Arguments to parse
     * @param handler DSLink handler
     * @return A link provider or null if the args have a help parameter
     * passed in.
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
     * @param name      Name of the link, prepended in the DsId.
     * @param args      Arguments to parse.
     * @param handler   DSLink handler.
     * @param requester Whether the generated link is a requester.
     * @param responder Whether the generated link is a responder.
     * @return A link provider or null if the args have a help parameter
     * passed in.
     */
    @SuppressWarnings("ConstantConditions")
    public static DSLinkProvider generate(String name,
                                          String[] args,
                                          DSLinkHandler handler,
                                          boolean requester,
                                          boolean responder) {
        // Log level is set until the configuration is read and sets
        // log level to the designated level.
        LogLevel.setLevel(Level.ERROR);
        handler.setConfig(Configuration.autoConfigure(name, args, requester, responder));
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
