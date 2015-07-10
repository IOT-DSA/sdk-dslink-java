package org.dsa.iot.historian.stats.rollup;

import org.dsa.iot.dslink.node.value.Value;

/**
 * @author Samuel Grenier
 */
public abstract class Rollup {

    /**
     * Resets the rollup for consumption again
     */
    public abstract void reset();

    /**
     * Updates the rollup data.
     *
     * @param value Value to update.
     * @param ts Timestamp of the value.
     */
    public abstract void update(Value value, long ts);

    /**
     * @return The statistical value of the rollup.
     */
    public abstract Value getValue();
}
