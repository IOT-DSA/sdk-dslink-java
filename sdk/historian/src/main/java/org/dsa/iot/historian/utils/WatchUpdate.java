package org.dsa.iot.historian.utils;

import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.historian.database.Watch;

/**
 * @author Samuel Grenier
 */
public class WatchUpdate {

    private final Watch watch;
    private final SubscriptionValue update;
    private long intervalTimestamp;

    public WatchUpdate(Watch watch, SubscriptionValue update) {
        this.watch = watch;
        this.update = update;
    }

    public Watch getWatch() {
        return watch;
    }

    public SubscriptionValue getUpdate() {
        return update;
    }

    public long getIntervalTimestamp() {
        return intervalTimestamp;
    }

    public void updateTimestamp(long nowTimestamp) {
        intervalTimestamp = nowTimestamp;
    }
}
