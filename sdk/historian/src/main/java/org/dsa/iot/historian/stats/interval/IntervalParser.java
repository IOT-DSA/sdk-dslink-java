package org.dsa.iot.historian.stats.interval;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Samuel Grenier
 */
public class IntervalParser {

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

    private long incrementTime;

    /**
     * @return The total amount of time combined to increment by.
     */
    public long incrementTime() {
        return incrementTime;
    }

    /**
     * @param ts Timestamp to align
     * @return Aligned timestamp
     */
    public long alignTime(long ts) {
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

    public static IntervalParser parse(String interval) {
        if (interval == null
                || "none".equals(interval)
                || "default".equals(interval)) {
            return null;
        }
        final IntervalParser parser = new IntervalParser();
        char[] chars = interval.toCharArray();
        StringBuilder number = new StringBuilder();
        for (char c : chars) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                parser.update(c, number.toString());
                number.delete(0, number.length());
            }
        }

        if (number.length() > 0) {
            throw new RuntimeException("Invalid expression");
        }

        parser.finishParsing();
        return parser;
    }
}
