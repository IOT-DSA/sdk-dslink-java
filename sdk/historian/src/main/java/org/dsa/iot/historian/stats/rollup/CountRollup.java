package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class CountRollup extends Rollup {

    private long count;

    @Override
    public void reset() {
        count = 0;
    }

    @Override
    public void update(Value value, long ts) {
        count++;
    }

    @Override
    public Value getValue() {
        return new Value(count);
    }
}
