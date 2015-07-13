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
            NodeBuilder b = node.createChild("setDisplayName");
            b.setDisplayName("Set Display Name");
            b.setSerializable(false);

            NameHandler handler = new NameHandler();
            Action a = new Action(perm, handler);
            handler.setAction(a);
            {
                Value def = null;
                String dn = node.getDisplayName();
                if (dn != null) {
                    def = new Value(dn);
                }
                Parameter p = new Parameter("Name", ValueType.STRING, def);
                String desc = "Sets the display name of the watch group\n";
                desc += "Leaving the name blank will remove the display name\n";
                desc += "This does not change the internal name";
                p.setDescription(desc);
                a.addParameter(p);
                handler.setParam(p);
            }
            b.setAction(a);
            b.build();
        }
    }

    private static class NameHandler implements Handler<ActionResult> {
        private Action action;
        private Parameter param;

        void setAction(Action action) {
            this.action = action;
        }

        void setParam(Parameter param) {
            this.param = param;
        }

        @Override
        public void handle(ActionResult event) {
            Value vn = event.getParameter("Name", ValueType.STRING);
            param.setDefaultValue(vn);
            {
                List<Parameter> params = new ArrayList<>();
                params.add(param);
                action.setParams(params);
            }

            Node node = event.getNode().getParent();
            if (vn != null) {
                node.setDisplayName(vn.getString());
            } else {
                node.setDisplayName(null);
            }
        }
    }
}
