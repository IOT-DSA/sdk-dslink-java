package org.dsa.iot.dslink;

import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.util.Configuration;

/**
 * Top level API for handling the configuration of nodes and responses to
 * requests.
 *
 * @author Samuel Grenier
 */
public abstract class DSLinkHandler {

    private final Configuration configuration;

    /**
     * Sets up a listener for the DSLink.
     *
     * @param configuration Configuration of the link
     */
    public DSLinkHandler(Configuration configuration) {
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
     * @param link The link that has completed a connection.
     */
    public abstract void onConnected(DSLink link);

    /**
     * Handles an incoming list response.
     *
     * @param request  Original sent request.
     * @param response Received response from the endpoint.
     */
    public void onListResponse(ListRequest request, ListResponse response) {
    }
}
