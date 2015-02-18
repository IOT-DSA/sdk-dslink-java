package org.dsa.iot.dslink.util;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.connection.Client;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.node.NodeManager;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public abstract class Linkable {

    @Getter
    private final EventBus bus;

    private ClientConnector clientConnector;
    private ServerConnector serverConnector;

    @Getter
    private NodeManager manager;

    public Linkable(@NonNull EventBus bus) {
        this.bus = bus;
    }

    public abstract void parse(Client client, JsonArray array);

    /**
     * A link must hold onto this in order to check for connectivity
     * and write to the server. Note that the node manager will be overwritten
     * when setting the connector.
     * @param cc Client connector to be set.
     * @param sc Server connector to be set.
     */
    public void setConnector(ClientConnector cc,
                             ServerConnector sc,
                             NodeManager manager) {
        checkConnected();
        this.clientConnector = cc;
        this.serverConnector = sc;
        this.manager = manager;
    }

    protected void checkConnected() {
        val err = "Already connected";
        if (clientConnector != null && clientConnector.isConnected()) {
            throw new IllegalStateException(err);
        } else if (serverConnector != null && serverConnector.isListening()) {
            throw new IllegalStateException(err);
        }
    }

    protected void ensureConnected() {
        val err = "Not connected";
        if (clientConnector != null && !clientConnector.isConnected()) {
            throw new IllegalStateException(err);
        } else if (serverConnector != null && !serverConnector.isListening()) {
            throw new IllegalStateException(err);
        }
    }
}
