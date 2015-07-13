package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.Handler;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public abstract class DatabaseProvider {

    /**
     * The action handler must set the database settings onto the node
     * configurations.
     *
     * @param perm Designated permission of the action.
     * @return The action with parameters necessary to create a database.
     */
    public abstract Action createDbAction(Permission perm);

    /**
     * @param node Node with initialized database connection settings.
     * @return A provided database to operate on.
     */
    protected abstract Database createDb(Node node);

    /**
     * @return A provided watch group.
     */
    protected abstract WatchGroup createWatchGroup();

    /**
     * Required permission level to create and modify databases.
     *
     * @return Required permission level.
     */
    public abstract Permission dbPermission();

    /**
     * Called before actually creating the database and typically used in the
     * action handler of {@link #createDbAction}.
     *
     * @param name Name of the database.
     * @param res Invocation result to retrieve the necessary node to create
     *            the database node.
     * @return An incomplete node builder that must be built after initialized.
     */
    public NodeBuilder createDbNode(String name, ActionResult res) {
        Node parent = res.getNode().getParent();
        if (parent.hasChild(name)) {
            throw new RuntimeException("Database already exists: " + name);
        }
        return parent.createChild(name);
    }

    /**
     * Creates the database and typically used in the action handler of
     * {@link #createDbAction} after {@link #createDbNode} has been called.
     *
     * @param node Database node to initialize.
     * @return An initialized database that has been connected to.
     * @see #createDb(Node) For creating the node.
     */
    public Database createAndInitDb(final Node node) {
        node.setRoConfig("db", new Value(true));
        final Database db = createDb(node);
        db.connect(new Handler<Database>() {
            @Override
            public void handle(Database event) {
                db.initExtensions(node);
            }
        });
        node.setMetaData(db);
        initCreateWatchGroupAct(node);
        initDeleteAct(node);

        // Handle watch groups
        Map<String, Node> children = node.getChildren();
        if (children != null) {
            for (Node child : children.values()) {
                Value v = child.getRoConfig("wg");
                if (v != null && v.getBool()) {
                    createAndInitWatchGroup(child);
                }
            }
        }

        return db;
    }

    private void createAndInitWatchGroup(Node node) {
        WatchGroup group = createWatchGroup();
        group.initSettings(dbPermission(), node);
    }

    private void initCreateWatchGroupAct(final Node node) {
        NodeBuilder b = node.createChild("createWatchGroup");
        b.setDisplayName("Create Watch Group");
        b.setSerializable(false);

        Action a = new Action(dbPermission(), new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Value vName = event.getParameter("Name", ValueType.STRING);
                String name = vName.getString();

                Node node = event.getNode().getParent();
                NodeBuilder b = node.createChild(name);
                b.setRoConfig("wg", new Value(true));
                createAndInitWatchGroup(b.build());
            }
        });
        {
            Parameter p = new Parameter("Name", ValueType.STRING);
            p.setDescription("The path to start watching and recording");
            a.addParameter(p);
        }

        b.setAction(a);
        b.build();
    }

    private void initDeleteAct(final Node node) {
        NodeBuilder b = node.createChild("deleteDb");
        b.setDisplayName("Delete Database");
        b.setSerializable(false);
        b.setAction(new Action(dbPermission(), new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Database db = node.getMetaData();
                try {
                    db.close();
                } catch (Exception ignored) {
                }
                Node child = event.getNode().getParent();
                child.getParent().removeChild(child);
            }
        }));
        b.build();
    }
}
