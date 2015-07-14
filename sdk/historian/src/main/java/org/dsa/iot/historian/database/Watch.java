package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Watch {

    private final WatchGroup group;
    private Node realTimeValue;

    public Watch(WatchGroup group) {
        this.group = group;
    }

    public void init(Permission perm, Node node) {
        initData(node);

        NodeBuilder b = node.createChild("unsubscribe");
        b.setSerializable(false);
        b.setDisplayName("Unsubscribe");
        b.setAction(new Action(perm, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Node node = event.getNode().getParent();
                node.getParent().removeChild(node);

                String path = node.getName().replaceAll("%2F", "/");
                SubscriptionPool pool = group.getDb().getProvider().getPool();
                pool.unsubscribe(path, Watch.this);
            }
        }));
        b.build();
    }

    protected void initData(Node node) {
        {
            realTimeValue = node;
            realTimeValue.setValueType(ValueType.DYNAMIC);
        }
        {

        }
        // TODO: start date
        // TODO: end date
        // TODO: enable/disable actions
        // TODO: last written value
    }

    /**
     * Called when the watch receives data.
     *
     * @param sv Received data.
     */
    public void onData(SubscriptionValue sv) {
        Value v = sv.getValue();
        realTimeValue.setValue(v);
    }
}
