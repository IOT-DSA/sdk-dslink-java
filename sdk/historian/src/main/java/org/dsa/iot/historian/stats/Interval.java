package org.dsa.iot.historian.stats;

import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.historian.stats.rollup.*;
import org.dsa.iot.historian.utils.TimeParser;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Samuel Grenier
 */
public class Interval {

    private final Rollup rollup;

    private int seconds = -1;
    private boolean alignSeconds;

    private int minutes = -1;
    private boolean alignMinutes;

    private int hours = -1;
    private boolean alignHours;

    private int days = -1;
    private boolean alignDays;

    private int weeks = -1;
    private boolean alignWeeks;

    private int months = -1;
    private boolean alignMonths;

    private int years = -1;
    private boolean alignYears;

    /**
     * The total amount of time combined to increment by.
     */
    private long incrementTime;

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

    private Interval(Rollup rollup) {
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
        final long alignedTs = alignTime(fullTs);

        Row row = null;
        if (alignedTs - lastValueTimeTrunc < incrementTime) {
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
        if (alignedTs - lastValueTimeTrunc >= incrementTime) {
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

    private long alignTime(long ts) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(ts);

        if (alignSeconds) {
            c.set(Calendar.MILLISECOND, 0);
        }

        if (alignMinutes) {
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        if (alignHours) {
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        if (alignDays) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        if (alignWeeks) {
            c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        if (alignMonths) {
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        if (alignYears) {
            c.set(Calendar.MONTH, 0);
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        return c.getTime().getTime();
    }

    private void setRealTime(Value value, long time) {
        // Subtract the increment time since the time must point to the
        // start time of the row, not the end.
        realTimeTime = time - incrementTime;
        realTimeValue = value;
    }

    private void update(char interval, String number) {
        int num = Integer.parseInt(number);
        switch (interval) {
            case 'S':
                alignSeconds = true;
            case 's':
                check("seconds", seconds);
                seconds = num;
                break;
            case 'M':
                alignMinutes = true;
            case 'm':
                check("minutes", minutes);
                minutes = num;
                break;
            case 'H':
                alignHours = true;
            case 'h':
                check("hours", hours);
                hours = num;
                break;
            case 'D':
                alignDays = true;
            case 'd':
                check("days", days);
                days = num;
                break;
            case 'W':
                alignWeeks = true;
            case 'w':
                check("weeks", weeks);
                weeks = num;
                break;
            case 'N':
                alignMonths = true;
            case 'n':
                check("months", months);
                months = num;
                break;
            case 'Y':
                alignYears = true;
            case 'y':
                check("years", years);
                years = num;
                break;
            default:
                throw new RuntimeException("Unknown char: " + interval);
        }
    }

    private void finishParsing() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(0);

        if (years > -1) {
            c.add(Calendar.YEAR, years);
        }

        if (months > -1) {
            c.add(Calendar.MONTH, months);
        }

        if (weeks > -1) {
            c.add(Calendar.WEEK_OF_MONTH, weeks);
        }

        if (days > -1) {
            c.add(Calendar.DAY_OF_MONTH, days);
        }

        if (hours > -1) {
            c.add(Calendar.HOUR, hours);
        }

        if (minutes > -1) {
            c.add(Calendar.MINUTE, minutes);
        }

        if (seconds > -1) {
            c.add(Calendar.SECOND, seconds);
        }

        incrementTime = c.getTime().getTime();
    }

    private void check(String type, int num) {
        if (num != -1) {
            throw new RuntimeException(type + " is already set");
        }
    }

    public static Interval parse(String interval, Rollup.Type rollup) {
        if (interval == null
                || "none".equals(interval)
                || "default".equals(interval)) {
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

        final Interval i = new Interval(roll);
        char[] chars = interval.toCharArray();
        StringBuilder number = new StringBuilder();
        for (char c : chars) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                i.update(c, number.toString());
                number.delete(0, number.length());
            }
        }

        if (number.length() > 0) {
            throw new RuntimeException("Invalid expression");
        }

        i.finishParsing();
        return i;
    }
}
