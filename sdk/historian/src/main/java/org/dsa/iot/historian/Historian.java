package org.dsa.iot.historian;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.historian.database.Database;
import org.dsa.iot.historian.database.DatabaseProvider;
import org.dsa.iot.historian.database.SubscriptionPool;
import org.dsa.iot.historian.database.WatchGroup;
import org.dsa.iot.historian.stats.GetHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public abstract class Historian extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Historian.class);
    private DatabaseProvider provider;
    private Node respSuperRoot;

    @Override
    public final boolean isRequester() {
        return true;
    }

    @Override
    public final boolean isResponder() {
        return true;
    }

    @Override
    public void stop() {
        if (respSuperRoot != null) {
            Map<String, Node> children = respSuperRoot.getChildren();
            if (children != null) {
                for (final Node n : children.values()) {
                    Database db = n.getMetaData();
                    if (db == null) {
                        continue;
                    }

                    try {
                        db.close();
                    } catch (Exception e) {
                        LOGGER.debug(e.getMessage());
                    }

                    Map<String, Node> wgs = n.getChildren();
                    if (wgs == null) {
                        continue;
                    }
                    for (Node wg : wgs.values()) {
                        WatchGroup g = wg.getMetaData();
                        if (g != null) {
                            g.close();
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @return A database provider.
     */
    public abstract DatabaseProvider createProvider();

    @Override
    public void onResponderInitialized(DSLink link) {
        provider = createProvider();
        respSuperRoot = link.getNodeManager().getSuperRoot();
        initialize(respSuperRoot);
        initHistoryProfile();
    }

    @Override
    public void onRequesterConnected(DSLink link) {
        provider.setPool(new SubscriptionPool(link.getRequester()));
        provider.subscribe(respSuperRoot);
        LOGGER.info("Connected");
    }

    /**
     * Initializes the node and all its children as a historian.
     *
     * @param node Historian root node.
     */
    public void initialize(Node node) {
        initAddDb(node);
        iterateDatabaseChildren(node);
    }

    protected void initHistoryProfile() {
        NodeBuilder b = respSuperRoot.createChild("defs");
        b.setSerializable(false);
        b.setHidden(true);
        Node node = b.build();

        b = node.createChild("profile");
        node = b.build();

        b = node.createChild("getHistory_");
        Action act = new Action(Permission.READ, null);
        GetHistory.initProfile(act);
        b.setAction(act);
        b.build();
    }

    /**
     * Initializes the create database action.
     *
     * @param node Node to initialize database creation.
     */
    protected void initAddDb(Node node) {
        NodeBuilder b = node.createChild("addDb");
        b.setSerializable(false);
        b.setDisplayName("Add Database");
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
     * @param args Arguments passed in from the program start.
     */
    public void start(String[] args) {
        DSLinkFactory.start(args, this);
    }
}
