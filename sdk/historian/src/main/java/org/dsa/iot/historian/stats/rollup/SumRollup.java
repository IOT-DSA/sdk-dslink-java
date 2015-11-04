package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class SumRollup extends Rollup {

    private double sum;

    @Override
    public void reset() {
        sum = 0;
    }

    @Override
    public void update(Value value, long ts) {
        Number number = value.getNumber();
        if (number != null) {
            sum += number.doubleValue();
        }
    }

    @Override
    public Value getValue() {
        return new Value(sum);
    }
}
