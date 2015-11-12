package org.dsa.iot.historian.stats;

/**
 * @author Samuel Grenier
 */
public class IntervalFactory {

    private static IntervalFactory DEFAULT;

    /**
     *
     * @param interval Specified interval of the data.
     * @param rollup Rollup that the interval operates on.
     * @return A created interval instance.
     */
    public Interval create(String interval, String rollup) {
        return Interval.parse(interval, rollup);
    }

    public static IntervalFactory getDefault() {
        if (DEFAULT == null) {
            initializeDefault();
        }
        return DEFAULT;
    }

    private static synchronized void initializeDefault() {
        if (DEFAULT == null) {
            DEFAULT = new IntervalFactory();
        }
    }
}
