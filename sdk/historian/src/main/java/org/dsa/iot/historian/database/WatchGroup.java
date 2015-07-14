package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class WatchGroup {

    /**
     * All settings must be as actions and their default parameters should be
     * updated when changed. Be sure to call {@code super} on this method.
     *
     * @param perm Permissions to modify settings.
     * @param node Watch group node to add settings to.
     */
    public void initSettings(Permission perm, Node node) {
        {
            String childName = "bufferFlushTime";
            NodeBuilder b = node.createChild(childName);
            b.setDisplayName("Buffer Flush Time");
            b.setValueType(ValueType.NUMBER);
            Value def = new Value(5);
            b.setValue(def);

            final Action act = new Action(perm, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Value val = event.getParameter("Time", ValueType.NUMBER);
                    Node node = event.getNode();
                    node.setValue(val);
                }
            });

            final Parameter p;
            {
                Node n = b.getParent().getChild(childName);
                if (n != null) {
                    def = n.getValue();
                }

                p = new Parameter("Time", ValueType.NUMBER, def);
                String desc = "Buffer flush time controls the interval when ";
                desc += "data gets written into the database\n";
                desc += "Setting a time to 0 means to record data immediately";
                p.setDescription(desc);
                act.addParameter(p);
            }
            b.getListener().setValueHandler(new Handler<ValuePair>() {
                @Override
                public void handle(ValuePair event) {
                    if (event.getCurrent().getNumber().intValue() < 0) {
                        event.setCurrent(new Value(0));
                    }
                    p.setDefaultValue(event.getCurrent());
                    List<Parameter> params = new ArrayList<>();
                    params.add(p);
                    act.setParams(params);
                }
            });
            b.setAction(act);
            b.build();
        }
        {
            NodeBuilder b = node.createChild("addWatchPath");
            b.setDisplayName("Add Watch Path");
            b.setSerializable(false);
            {
                Action a = new Action(perm, new Handler<ActionResult>() {
                    @Override
                    public void handle(ActionResult event) {
                        // TODO: create watch
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
    }
}
