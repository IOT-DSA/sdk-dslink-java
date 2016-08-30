package org.dsa.iot.historian.stats.interval;

import org.dsa.iot.dslink.node.actions.table.*;
import org.dsa.iot.dslink.node.value.*;
import org.dsa.iot.dslink.util.*;
import org.dsa.iot.historian.stats.rollup.*;
import org.dsa.iot.historian.utils.*;
import java.util.*;

/**
 * @author Samuel Grenier
 */
public class IntervalProcessor {

    private Calendar calendar; // Calendar with the correct timezone.
    private long currentInterval = -1;  //Timestamp of the current interval.
    private QueryData lastValue; // The last value of the current interval.
    private long nextInterval = -1; //Timestamp of the next interval.
    private final IntervalParser parser;
    private final Rollup rollup;

    private IntervalProcessor(IntervalParser parser,
                              Rollup rollup,
                              TimeZone timeZone) {
        this.parser = parser;
        this.rollup = rollup;
        calendar = Calendar.getInstance();
        calendar.setTimeZone(timeZone);
    }

    /**
     * Update the current interval, but if the if the arguments represent a new
     * interval, then the prior will be returned.
     *
     * @param data   Value retrieved from the database.
     * @param fullTs Full timestamp of the value.
     * @return The last interval, or null.
     */
    public Row getRowUpdate(QueryData data, long fullTs) {
        calendar.setTimeInMillis(fullTs);
        parser.alignTime(calendar);
        long alignedTs = calendar.getTimeInMillis();
        if (currentInterval < 0) {
            currentInterval = alignedTs;
            parser.nextInterval(calendar);
            nextInterval = calendar.getTimeInMillis();
        }
        Row row = null;
        if (alignedTs < currentInterval) { //Out of order timestamp, ignore it.
            return null;
        }
        if (alignedTs < nextInterval) { // Update the current interval
            if (rollup != null) {
                rollup.update(data.getValue(), currentInterval);
            }
            lastValue = data;
            return null;
        }
        if (lastValue != null) { // Finish the last interval
            if (rollup == null) {
                row = makeRow(data.getValue(), currentInterval);
            } else {
                row = makeRow(rollup.getValue(), currentInterval);
            }
            lastValue = null;
        }
        while (alignedTs >= nextInterval) { //Advance to the next interval
            currentInterval = nextInterval;
            calendar.setTimeInMillis(currentInterval);
            parser.nextInterval(calendar);
            nextInterval = calendar.getTimeInMillis();
        }
        if (rollup != null) {
            rollup.reset();
            rollup.update(data.getValue(), currentInterval);
        }
        lastValue = data;
        return row;
    }

    /**
     * Returns a row representing the current interval, or null if the current
     * interval has no data.
     */
    public Row complete() {
        if (lastValue == null) { //There is no data in the current interval.
            return null;
        }
        // Finish the current interval
        Row row = null;
        if (rollup == null) {
            row = makeRow(lastValue.getValue(), currentInterval);
        } else {
            row = makeRow(rollup.getValue(), currentInterval);
        }
        lastValue = null;
        return row;
    }

    private Row makeRow(Value value, long ts) {
        Row row = new Row();
        calendar.setTimeInMillis(ts);
        row.addValue(new Value(TimeUtils.encode(calendar, true, null).toString()));
        row.addValue(value);
        return row;
    }

    public static IntervalProcessor parse(IntervalParser parser,
                                          Rollup.Type rollup,
                                          TimeZone timeZone) {
        if (parser == null) {
            return null;
        }
        Rollup roll = null;
        if (Rollup.Type.AND == rollup) {
            roll = new AndRollup();
        } else if (Rollup.Type.OR == rollup) {
            roll = new OrRollup();
        } else if (Rollup.Type.AVERAGE == rollup) {
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
        return new IntervalProcessor(parser, roll, timeZone);
    }
}
