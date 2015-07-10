package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class LastRollup extends Rollup {

    private long ts = -1;
    private Value value;

    @Override
    public void reset() {
        value = null;
    }

    @Override
    public void update(Value value, long ts) {
        if (ts > this.ts) {
            this.ts = ts;
            this.value = value;
        }
    }

    @Override
    public Value getValue() {
        return value;
    }
}
