package org.dsa.iot.historian.stats.interval;

import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.historian.stats.rollup.*;
import org.dsa.iot.historian.utils.TimeParser;

/**
 * @author Samuel Grenier
 */
public class IntervalProcessor {

    private final IntervalParser parser;
    private final Rollup rollup;

    /**
     * Last timestamp of the last value.
     */
    private long lastValueTimeTrunc = -1;

    /**
     * Last updated value used for roll ups.
     */
    private Value lastValue;

    /**
     * Last real time value.
     */
    private Value realTimeValue;

    /**
     * Last real time timestamp.
     */
    private long realTimeTime;

    private IntervalProcessor(IntervalParser parser,
                              Rollup rollup) {
        this.parser = parser;
        this.rollup = rollup;
    }

    /**
     *
     * @param value Value retrieved from the database.
     * @param fullTs Full timestamp of the value.
     * @return An update or null to skip the update.
     */
    public Row getRowUpdate(Value value, long fullTs) {
        setRealTime(value, fullTs);
        final long alignedTs = parser.alignTime(fullTs);

        Row row = null;
        if (alignedTs - lastValueTimeTrunc < parser.incrementTime()) {
            // Update the last value within the same interval
            lastValue = value;
            if (rollup != null) {
                rollup.update(value, fullTs);
            }
        } else if (lastValue != null) {
            // Finish up the rollup, the interval for this period is completed
            if (rollup == null) {
                row = makeRow(lastValue, lastValueTimeTrunc);
            } else {
                row = makeRow(rollup.getValue(), lastValueTimeTrunc);
            }
            lastValue = null;
            setRealTime(value, alignedTs);
        }

        // New interval period has been started
        if (alignedTs - lastValueTimeTrunc >= parser.incrementTime()) {
            lastValueTimeTrunc = alignedTs;
            lastValue = value;
            if (rollup != null) {
                rollup.reset();
                rollup.update(lastValue, fullTs);
            }
        }
        return row;
    }

    public Row complete() {
        if (lastValue == null) {
            // Don't duplicate the value if the interval finished and there
            // is no more data.
            return null;
        }
        Value val = realTimeValue;
        if (val != null) {
            if (rollup != null) {
                val = rollup.getValue();
            }
            return makeRow(val, realTimeTime);
        }
        return null;
    }

    private Row makeRow(Value value, long ts) {
        Row row = new Row();
        row.addValue(new Value(TimeParser.parse(ts)));
        row.addValue(value);
        return row;
    }

    private void setRealTime(Value value, long time) {
        // Subtract the increment time since the time must point to the
        // start time of the row, not the end.
        realTimeTime = time - parser.incrementTime();
        realTimeValue = value;
    }

    public static IntervalProcessor parse(IntervalParser parser,
                                          Rollup.Type rollup) {
        if (parser == null) {
            return null;
        }
        Rollup roll = null;
        if (Rollup.Type.AVERAGE == rollup) {
            roll = new AvgRollup();
        } else if (Rollup.Type.COUNT == rollup) {
            roll = new CountRollup();
        } else if (Rollup.Type.FIRST == rollup) {
            roll = new FirstRollup();
        } else if (Rollup.Type.LAST == rollup) {
            roll = new LastRollup();
        } else if (Rollup.Type.MAX == rollup) {
            roll = new MaxRollup();
        } else if (Rollup.Type.MIN == rollup) {
            roll = new MinRollup();
        } else if (Rollup.Type.SUM == rollup) {
            roll = new SumRollup();
        } else if (Rollup.Type.DELTA == rollup) {
            roll = new DeltaRollup();
        } else if (Rollup.Type.NONE != rollup) {
            throw new RuntimeException("Invalid rollup: " + rollup);
        }
        return new IntervalProcessor(parser, roll);
    }
}
