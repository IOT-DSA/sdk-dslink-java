package org.dsa.iot.historian.stats.interval;

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
        if (alignSeconds) {
            ts -= (ts % 1e+3);
        }

        if (alignMinutes) {
            ts -= (ts % 6e+4);
        }

        if (alignHours) {
            ts -= (ts % 3.6e6);
        }

        if (alignDays) {
            ts -= (ts % 8.64e+7);
        }

        if (alignWeeks) {
            ts -= (ts % 6.048e+8);
        }

        if (alignMonths) {
            ts -= (ts % 2.628e+9);
        }

        if (alignYears) {
            ts -= (ts % 3.154e+10);
        }

        return ts;
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
        long ts = 0;

        if (years > -1) {
            ts += (3.154e+10 * years);
        }

        if (months > -1) {
            ts += (2.628e+9 * months);
        }

        if (weeks > -1) {
            ts += (6.048e+8 * weeks);
        }

        if (days > -1) {
            ts += (8.64e+7 * days);
        }

        if (hours > -1) {
            ts += (3.6e+6 * hours);
        }

        if (minutes > -1) {
            ts += (6e+4 * minutes);
        }

        if (seconds > -1) {
            ts += (1e+3 * seconds);
        }

        incrementTime = ts;
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
