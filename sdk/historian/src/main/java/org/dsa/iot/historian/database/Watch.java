package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.historian.utils.QueryData;
import org.dsa.iot.historian.utils.TimeParser;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Watch {

    private final WatchGroup group;
    private Node realTimeValue;
    private boolean enabled;
    private String path;

    public Watch(WatchGroup group) {
        this.group = group;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void init(Permission perm, Node node) {
        path = node.getName().replaceAll("%2F", "/");
        initData(node);

        NodeBuilder b = node.createChild("unsubscribe");
        b.setSerializable(false);
        b.setDisplayName("Unsubscribe");
        b.setAction(new Action(perm, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                Node node = event.getNode().getParent();
                node.getParent().removeChild(node);

                SubscriptionPool pool = group.getDb().getProvider().getPool();
                pool.unsubscribe(path, Watch.this);
            }
        }));
        b.build();
    }

    protected void initData(final Node node) {
        {
            realTimeValue = node;
            realTimeValue.setValueType(ValueType.DYNAMIC);
        }

        {
            NodeBuilder b = node.createChild("enabled");
            b.setDisplayName("Enabled");
            b.setWritable(Writable.CONFIG);
            b.setValueType(ValueType.BOOL);
            b.setValue(new Value(true));
            b.getListener().setValueHandler(new Handler<ValuePair>() {
                @Override
                public synchronized void handle(ValuePair event) {
                    enabled = event.getCurrent().getBool();
                    String path = node.getName().replaceAll("%2F", "/");
                    SubscriptionPool pool = group.getDb().getProvider().getPool();
                    if (enabled) {
                        pool.subscribe(path, Watch.this);
                    } else {
                        pool.unsubscribe(path, Watch.this);
                    }
                }
            });
            Node n = b.build();
            enabled = n.getValue().getBool();
        }

        {
            NodeBuilder b = node.createChild("startDate");
            b.setDisplayName("Start Date");
            b.setValueType(ValueType.TIME);
            {
                QueryData data = group.getDb().queryFirst(path);
                if (data != null) {
                    Value v = new Value(TimeParser.parse(data.getTimestamp()));
                    b.setValue(v);
                }
                // TODO: set start date when it never existed.
            }
            b.build();
        }

        {
            NodeBuilder b = node.createChild("endDate");
            b.setDisplayName("End Date");
            b.setValueType(ValueType.TIME);
            // TODO: provide a value setter for this node
            Node n = b.build();
            {
                QueryData data = group.getDb().queryLast(path);
                if (data != null) {
                    Value v = new Value(TimeParser.parse(data.getTimestamp()));
                    n.setValue(v);
                }
            }
        }

        {
            NodeBuilder b = node.createChild("lwv");
            b.setDisplayName("Last Written Value");
            b.setValueType(ValueType.DYNAMIC);
            // TODO: provide a value setter for this node
            b.build();
        }
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
