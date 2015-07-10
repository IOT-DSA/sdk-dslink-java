package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class DeltaRollup extends Rollup {

    private Number start;
    private Number end;

    @Override
    public void reset() {
        start = null;
        end = null;
    }

    @Override
    public void update(Value value, long ts) {
        if (start == null) {
            start = value.getNumber();
        }
        end = value.getNumber();
    }

    @Override
    public Value getValue() {
        if (start != null && end != null) {
            double delta = end.doubleValue() - start.doubleValue();
            if (end.doubleValue() < start.doubleValue()) {
                delta = end.doubleValue();
            }
            return new Value(delta);
        } else if (start != null) {
            return new Value(0);
        } else if (end != null) {
            return new Value(end);
        }
        return null;
    }
}
