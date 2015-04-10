package org.dsa.iot.dslink;

import org.dsa.iot.dslink.config.Configuration;

/**
 * Top level API for handling the configuration of nodes and responses to
 * requests. Note that {@link #onRequesterInitialized} and
 * {@link #onResponderInitialized} can be each called for the same link ID.
 * This allows for the node managers to be completely isolated between a
 * a requester and responder.
 *
 * @author Samuel Grenier
 */
public abstract class DSLinkHandler {

    private Configuration configuration;

    /**
     * Sets the configuration of the handler.
     *
     * @param configuration Configuration of the link
     */
    public void setConfig(Configuration configuration) {
        if (configuration == null)
            throw new NullPointerException("configuration");
        this.configuration = configuration;
    }

    /**
     * @return Configuration of the DSLink
     */
    public Configuration getConfig() {
        return configuration;
    }

    /**
     * Pre initializes the handler. If this link is a responder any actions
     * must be populated here.
     */
    public void preInit() {
    }

    /**
     * This method is asynchronously called. The link is not yet connected
     * to the server.
     *
     * @param link The link that has completed a connection.
     */
    public void onRequesterInitialized(DSLink link) {
    }

    /**
     * This method is asynchronously called. The link is not yet connected
     * to the server.
     *
     * @param link The link that needs to be initialized.
     */
    public void onResponderInitialized(DSLink link) {
    }
}
