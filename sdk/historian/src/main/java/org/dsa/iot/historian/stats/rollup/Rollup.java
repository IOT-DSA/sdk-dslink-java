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

    public enum Type {
        AVERAGE("avg"),
        COUNT("count"),
        FIRST("first"),
        LAST("last"),
        MAX("max"),
        MIN("min"),
        SUM("sum"),
        DELTA("delta"),
        NONE("none");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public static Type toEnum(String rollup) {
            if (AVERAGE.name.equals(rollup)) {
                return AVERAGE;
            } else if (COUNT.name.equals(rollup)) {
                return COUNT;
            } else if (FIRST.name.equals(rollup)) {
                return FIRST;
            } else if (LAST.name.equals(rollup)) {
                return LAST;
            } else if (MAX.name.equals(rollup)) {
                return MAX;
            } else if (MIN.name.equals(rollup)) {
                return MIN;
            } else if (SUM.name.equals(rollup)) {
                return SUM;
            } else if (DELTA.name.equals(rollup)) {
                return DELTA;
            } else if (NONE.name.equals(rollup)) {
                return NONE;
            }
            return null;
        }
    }
}
