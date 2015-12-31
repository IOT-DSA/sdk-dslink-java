package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

/**
 * @author Samuel Grenier
 */
public class AndRollup extends Rollup {

    private boolean value;

    @Override
    public void reset() {
        value = false;
    }

    @Override
    public void update(Value value, long ts) {
        if (value.getType().equals(ValueType.NUMBER)) {
            Number number = value.getNumber();
            if (number != null) {
                this.value &= (number.doubleValue() != 0);
            }
        } else {
            Boolean bool = value.getBool();
            if (bool != null) {
                this.value &= bool;
            }
        }
    }

    @Override
    public Value getValue() {
        return new Value(value);
    }
}
