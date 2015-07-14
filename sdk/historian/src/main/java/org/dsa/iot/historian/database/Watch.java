package org.dsa.iot.historian.database;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

/**
 * @author Samuel Grenier
 */
public class Watch {

    private final WatchGroup group;
    private Node realTimeValue;

    public Watch(WatchGroup group) {
        this.group = group;
    }

    public void initData(Node node) {
        {
            realTimeValue = node;
            realTimeValue.setValueType(ValueType.DYNAMIC);
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
