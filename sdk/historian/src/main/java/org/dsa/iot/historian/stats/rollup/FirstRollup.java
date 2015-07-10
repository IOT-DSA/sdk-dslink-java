package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class FirstRollup extends Rollup {

    private Value value;
    private long ts;

    @Override
    public void reset() {
        value = null;
        ts = 0;
    }

    @Override
    public void update(Value value, long ts) {
        if (this.value == null || this.ts > ts) {
            this.ts = ts;
            this.value = value;
        }
    }

    @Override
    public Value getValue() {
        return value;
    }
}
