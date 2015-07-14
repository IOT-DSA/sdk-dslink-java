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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Samuel Grenier
 */
public class WatchGroup {

    private final Permission permission;
    private final Database db;
    private final Node node;

    //private LoggingType type;
    //private int flushTime;
    //private long interval;

    /**
     * @param perm Permission all actions should be set to.
     * @param node Watch group node.
     * @param db Database the group writes to.
     */
    public WatchGroup(Permission perm, Node node, Database db) {
        this.permission = perm;
        this.node = node;
        this.db = db;
    }

    /**
     * @return The database the group operates on.
     */
    public Database getDb() {
        return db;
    }

    /**
     * Initializes the watch for the group.
     *
     * @param path Watch path.
     */
    protected void initWatch(String path) {
        Watch watch = new Watch(this);
        {
            NodeBuilder b = node.createChild(path.replaceAll("/", "%2F"));
            Node n = b.build();
            watch.init(permission, n);
            n.setMetaData(watch);
        }
        db.getProvider().getPool().subscribe(path, watch);
    }

    /**
     * Subscribes to the entire watch group.
     */
    public void subscribe() {
        Map<String, Node> children = node.getChildren();
        for (Node n : children.values()) {
            if (n.getAction() == null) {
                initWatch(n.getName().replaceAll("%2F", "/"));
            }
        }
    }

    /**
     * Unsubscribes from the entire watch group.
     */
    public void unsubscribe() {
        SubscriptionPool pool = db.getProvider().getPool();
        Map<String, Node> children = node.getChildren();
        for (Node n : children.values()) {
            if (n.getAction() == null) {
                String path = n.getName().replaceAll("%2F", "/");
                Watch w = n.getMetaData();
                pool.unsubscribe(path, w);
            }
        }
    }

    /**
     * All settings must be as actions and their default parameters should be
     * updated when changed. Be sure to call {@code super} on this method.
     */
    protected void initSettings() {
        {
            NodeBuilder b = node.createChild("addWatchPath");
            b.setDisplayName("Add Watch Path");
            {
                Action a = new Action(permission, new Handler<ActionResult>() {
                    @Override
                    public void handle(ActionResult event) {
                        ValueType vt = ValueType.STRING;
                        Value v = event.getParameter("Path", vt);
                        String path = v.getString();
                        initWatch(path);
                    }
                });
                {
                    Parameter p = new Parameter("Path", ValueType.STRING);
                    p.setDescription("Path to start watching for value changes");
                    a.addParameter(p);
                }
                b.setAction(a);
            }
            b.build();
        }
        {
            NodeBuilder b = node.createChild("edit");
            b.setDisplayName("Edit");
            // Buffer flush time
            b.setRoConfig("bft", new Value(5));
            // Logging type
            b.setRoConfig("lt", new Value(LoggingType.ALL_DATA.getName()));
            // Interval
            b.setRoConfig("i", new Value(5));

            final Parameter bft;
            {
                bft = new Parameter("Buffer Flush Time", ValueType.NUMBER);
                {
                    String desc = "Buffer flush time controls the interval ";
                    desc += "when data gets written into the database\n";
                    desc += "Setting a time to 0 means to record data ";
                    desc += "immediately";
                    bft.setDescription(desc);
                }
                bft.setDefaultValue(getValue(b, "bft"));
            }

            final Parameter lt;
            {
                Value v = getValue(b, "lt");
                Set<String> enums = LoggingType.buildEnums(v.getString());
                lt = new Parameter("Logging Type", ValueType.makeEnum(enums));
                lt.setDefaultValue(v);
                {
                    String desc = "Logging type controls what kind of data ";
                    desc += "gets stored into the database";
                    lt.setDescription(desc);
                }
            }

            final Parameter i;
            {
                i = new Parameter("Interval", ValueType.NUMBER);
                {
                    String desc = "Interval controls how long to wait before ";
                    desc += "buffering the next value update.\n";
                    desc += "This setting has no effect when logging type is ";
                    desc += "not interval.";
                    i.setDescription(desc);
                }
                i.setDefaultValue(getValue(b, "i"));
            }

            EditSettingsHandler handler = new EditSettingsHandler();
            {
                handler.setBufferFlushTimeParam(bft);
                handler.setLoggingTypeParam(lt);
                handler.setIntervalParam(i);

                Action a = new Action(permission, handler);
                a.addParameter(bft);
                a.addParameter(lt);
                a.addParameter(i);
                handler.setAction(a);

                b.setAction(a);
            }

            b.build();
        }
        {
            NodeBuilder b = node.createChild("delete");
            b.setDisplayName("Delete");
            b.setAction(new Action(permission, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Node node = event.getNode().getParent();
                    node.getParent().removeChild(node);
                    unsubscribe();
                }
            }));
            b.build();
        }
    }

    private Value getValue(NodeBuilder b, String name) {
        Node n = b.getParent().getChild(b.getChild().getName());
        if (n != null) {
            return n.getRoConfig(name);
        }
        return b.getChild().getRoConfig(name);
    }

    private static class EditSettingsHandler implements Handler<ActionResult> {

        private Action action;

        private Parameter bft;
        private Parameter lt;
        private Parameter i;

        public void setAction(Action a) {
            this.action = a;
        }

        public void setBufferFlushTimeParam(Parameter bft) {
            this.bft = bft;
        }

        public void setLoggingTypeParam(Parameter lt) {
            this.lt = lt;
        }

        public void setIntervalParam(Parameter i) {
            this.i = i;
        }

        @Override
        public void handle(ActionResult event) {
            Node node = event.getNode();

            Value vLt = event.getParameter(lt.getName(), ValueType.STRING);
            Value vBft = event.getParameter(bft.getName(), bft.getType());
            if (vBft.getNumber().intValue() < 0) {
                vBft.set(0);
            }

            Value vI = event.getParameter(i.getName(), i.getType());
            if (vI.getNumber().intValue() < 0) {
                vI.set(0);
            }

            node.setRoConfig("bft", vBft);
            bft.setDefaultValue(vBft);

            node.setRoConfig("lt", vLt);
            lt.setDefaultValue(vLt);

            node.setRoConfig("i", vI);
            i.setDefaultValue(vI);

            Set<String> enums = LoggingType.buildEnums(vLt.getString());
            lt.setValueType(ValueType.makeEnum(enums));

            List<Parameter> params = new LinkedList<>();
            params.add(bft);
            params.add(lt);
            params.add(i);
            action.setParams(params);
        }
    }
}
