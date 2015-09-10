package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public class DeltaRollup extends Rollup {

    private Number previousStart;
    private Number start;
    private Number end;

    @Override
    public void reset() {
        previousStart = start;
        start = null;
        end = null;
    }

    @Override
    public void update(Value value, long ts) {
        if (start == null) {
            start = value.getNumber();
        }
        if (previousStart == null) {
            previousStart = start;
        }
        end = value.getNumber();
    }

    @Override
    public Value getValue() {
        if (previousStart != null && end != null) {
            double delta = end.doubleValue() - previousStart.doubleValue();
            if (end.doubleValue() < previousStart.doubleValue()) {
                delta = end.doubleValue();
            }
            return new Value(delta);
        } else if (previousStart != null) {
            return new Value(0);
        } else if (end != null) {
            return new Value(end);
        }
        return null;
    }
}
