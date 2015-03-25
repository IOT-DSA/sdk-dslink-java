package org.dsa.iot.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionRegistry;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.ValueType;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    @Override
    public void preInit() {
        ActionRegistry registry = getActionRegistry();
        Permission perm = Permission.READ;

        {
            Action action = new Action("addRNG", perm, RNG.getAddHandler());
            action.addParameter(new Parameter("count", ValueType.NUMBER));
            registry.register(action);
        }

        {
            Action action = new Action("removeRNG", perm, RNG.getRemoveHandler());
            registry.register(action);
        }
    }

    @Override
    public void onResponderConnected(DSLink link) {
        NodeManager manager = link.getNodeManager();
        Node superRoot = manager.getNode("/").getNode();

        Replicator.start(superRoot);
        RNG.start(superRoot);
    }

    public static void main(String[] args) {
        DSLinkFactory.startResponder("responder", args, new Main());
    }
}
