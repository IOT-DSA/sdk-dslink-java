package org.dsa.iot.dslink.util;

import com.google.common.eventbus.EventBus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.vertx.java.core.json.JsonArray;

/**
 * @author Samuel Grenier
 */
public abstract class Linkable {

    @Getter
    private final EventBus bus;

    @Getter(AccessLevel.PROTECTED)
    private ClientConnector connector;

    @Getter
    private NodeManager manager;

    public Linkable(@NonNull EventBus bus) {
        this.bus = bus;
    }

    public abstract void parse(JsonArray array);

    /**
     * A link must hold onto this in order to check for connectivity
     * and write to the server. Note that the node manager will be overwritten
     * when setting the connector.
     * @param connector Connector to be set.
     */
    public void setConnector(ClientConnector connector) {
        val man = new SubscriptionManager(connector);
        setConnector(connector, new NodeManager(bus, man));
    }

    /**
     * A link must hold onto this in order to check for connectivity
     * and write to the server. Note that the node manager will be overwritten
     * when setting the connector.
     * @param connector Connector to be set.
     */
    public void setConnector(ClientConnector connector, NodeManager manager) {
        checkConnected();
        this.connector = connector;
        this.manager = manager;
    }

    protected void checkConnected() {
        if (connector != null && connector.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
    }

    protected void ensureConnected() {
        if (connector == null || !connector.isConnected()) {
            throw new IllegalStateException("Not connected");
        }
    }
}
