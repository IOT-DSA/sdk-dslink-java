package org.dsa.iot.historian;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.historian.database.DatabaseProvider;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public abstract class Historian extends DSLinkHandler {

    private final DatabaseProvider provider;

    /**
     * Constructs a historian DSLink.
     *
     * @param provider Database provider.
     */
    public Historian(DatabaseProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        this.provider = provider;
    }

    @Override
    public final void onResponderInitialized(DSLink link) {
        Node superRoot = link.getNodeManager().getSuperRoot();
        {
            NodeBuilder b = superRoot.createChild("createDb");
            b.setDisplayName("Create Database");
            b.setAction(provider.createDbAction(provider.dbPermission()));
            b.build();
        }

        Map<String, Node> children = superRoot.getChildren();
        if (children != null) {
            for (final Node node : children.values()) {
                Value v = node.getRoConfig("db");
                if (v != null && v.getBool()) {
                    provider.createAndInitDb(node);
                }
            }
        }
    }

    @Override
    public final void onRequesterConnected(DSLink link) {

    }

    /**
     * Starts the DSLink.
     *
     * @param name Name of the historian.
     * @param args Arguments passed in from the program start.
     */
    public void start(String name, String[] args) {
        DSLinkFactory.startDual(name, args, this);
    }
}
