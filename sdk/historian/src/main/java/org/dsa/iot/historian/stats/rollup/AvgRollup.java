package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class AvgRollup extends Rollup {

    private double total;
    private int count;

    @Override
    public void reset() {
        total = 0;
        count = 0;
    }

    @Override
    public void update(Value value, long ts) {
        count++;
        total += value.getNumber().doubleValue();
    }

    public Value getValue() {
        double avg = total / count;
        return new Value(avg);
    }
}
