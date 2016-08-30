package org.dsa.iot.historian.stats.interval;

import org.dsa.iot.dslink.util.*;
import java.util.*;

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

    /**
     * If configured to align to an interval, this will align the given calendar.
     *
     * @return True if the calendar was modified.
     */
    public boolean alignTime(Calendar calendar) {
        boolean modified = false;
        if (alignSeconds) {
            TimeUtils.alignSeconds(seconds,calendar);
            modified = true;
        }
        if (alignMinutes) {
            TimeUtils.alignMinutes(minutes,calendar);
            modified = true;
        }
        if (alignHours) {
            TimeUtils.alignHour(calendar);
            modified = true;
        }
        if (alignDays) {
            TimeUtils.alignDay(calendar);
            modified = true;
        }
        if (alignWeeks) {
            TimeUtils.alignWeek(calendar);
            modified = true;
        }
        if (alignMonths) {
            TimeUtils.alignMonth(calendar);
            modified = true;
        }
        if (alignYears) {
            TimeUtils.alignYear(calendar);
            modified = true;
        }
        return modified;
    }

    /**
     * Advances the calendar, without performing any alignment.
     *
     * @return True if the calendar was modified.
     */
    public boolean nextInterval(Calendar calendar) {
        boolean modified = false;
        if (seconds > 0) {
            TimeUtils.addSeconds(seconds,calendar);
            modified = true;
        }
        if (minutes > 0) {
            TimeUtils.addMinutes(minutes,calendar);
        }
        if (hours > 0) {
            TimeUtils.addHours(hours,calendar);
            modified = true;
        }
        if (days > 0) {
            TimeUtils.addDays(days,calendar);
            modified = true;
        }
        if (weeks > 0) {
            TimeUtils.addWeeks(weeks,calendar);
            modified = true;
        }
        if (months > 0) {
            TimeUtils.addMonths(months,calendar);
            modified = true;
        }
        if (years > 0) {
            TimeUtils.addYears(years,calendar);
            modified = true;
        }
        return modified;
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
        return parser;
    }
}
