package org.dsa.iot.responder.rng;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;

/**
 * @author Samuel Grenier
 */
public class TableActions {

    public static void init(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("tableStream");
        builder.setAction(Actions.getTableStreamAction());
        builder.setSerializable(false);
        builder.build();

        builder = superRoot.createChild("tableRefresh");
        builder.setAction(Actions.getTableRefreshAction());
        builder.setSerializable(false);
        builder.build();

        builder = superRoot.createChild("tableReplace");
        builder.setAction(Actions.getTableReplaceAction());
        builder.setSerializable(false);
        builder.build();

        builder = superRoot.createChild("table");
        builder.setAction(Actions.getTableAction());
        builder.setSerializable(false);
        builder.build();
    }

}
