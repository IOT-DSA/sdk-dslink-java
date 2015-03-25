package org.dsa.iot.responder;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.NodeBuilder;
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

        NodeBuilder builder = manager.createRootNode("replicator");
        Replicator replicator = new Replicator(builder.build());
        replicator.start();

        builder = manager.createRootNode("rng");
        RNG rng = new RNG(builder.build());
        rng.start();
    }

    public static void main(String[] args) {
        DSLinkFactory.startResponder("responder", args, new Main());
    }
}
