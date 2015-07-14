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
        initialize(superRoot);
    }

    @Override
    public final void onRequesterConnected(DSLink link) {

    }

    /**
     * Initializes the node and all its children as a historian.
     *
     * @param node Historian root node.
     */
    public void initialize(Node node) {
        initCreateDb(node);
        iterateDatabaseChildren(node);
    }

    /**
     * Initializes the create database action.
     *
     * @param node Node to initialize database creation.
     */
    protected void initCreateDb(Node node) {
        NodeBuilder b = node.createChild("createDb");
        b.setDisplayName("Create Database");
        b.setAction(provider.createDbAction(provider.dbPermission()));
        b.build();
    }

    /**
     * Iterates the children of the node and initializes the database.
     *
     * @param node Database container node.
     */
    protected void iterateDatabaseChildren(Node node) {
        Map<String, Node> children = node.getChildren();
        if (children != null) {
            for (final Node n : children.values()) {
                Value v = n.getRoConfig("db");
                if (v != null && v.getBool()) {
                    provider.createAndInitDb(n);
                }
            }
        }
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
