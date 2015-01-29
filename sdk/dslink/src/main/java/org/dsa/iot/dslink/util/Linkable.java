package org.dsa.iot.dslink.util;

import lombok.AccessLevel;
import lombok.Getter;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public abstract class Linkable {

    @Getter(AccessLevel.PROTECTED)
    private ClientConnector connector;

    public abstract void parse(JsonArray array);

    /**
     * A link must hold onto this in order to check for connectivity
     * and write to the server.
     * @param connector Connector to be set.
     */
    public synchronized void setConnector(ClientConnector connector) {
        checkConnected();
        this.connector = connector;
    }

    protected synchronized void checkConnected() {
        if (connector != null && connector.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
    }
}
