package org.dsa.iot.dslink.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Samuel Grenier
 */
public class TimeUtils {

    private static final ThreadLocal<DateFormat> FORMAT_TIME_ZONE;
    private static final ThreadLocal<DateFormat> FORMAT;
    private static final String TIME_PATTERN_TZ;
    private static final String TIME_PATTERN;
    private static final String TIME_ZONE_COLON;
    private static final String TIME_ZONE;

    static {
        long currentTime = new Date().getTime();
        int offset = TimeZone.getDefault().getOffset(currentTime) / (1000 * 60);
        String s = "+";
        if (offset < 0) {
            offset = -offset;
            s = "-";
        }
        int hh = offset / 60;
        int mm = offset % 60;
        TIME_ZONE_COLON = s + (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
        TIME_ZONE = TIME_ZONE_COLON.replace(":", "");
        TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        TIME_PATTERN_TZ = TIME_PATTERN + "Z";

        FORMAT = new ThreadLocal<DateFormat>() {
            @Override
            public DateFormat initialValue() {
                return new SimpleDateFormat(getTimePattern());
            }
        };
        FORMAT_TIME_ZONE = new ThreadLocal<DateFormat>() {
            @Override
            public DateFormat initialValue() {
                return new SimpleDateFormat(getTimePatternTz());
            }
        };
    }

    /**
     *  Do not allow instantiation.
     */
    private TimeUtils() { }

    public static String getTimePatternTz() {
        return TIME_PATTERN_TZ;
    }

    public static String getTimePattern() {
        return TIME_PATTERN;
    }

    public static String format(long time) {
        return format(new Date(time));
    }

    public static String format(Date time) {
        return FORMAT.get().format(time) + TIME_ZONE_COLON;
    }

    public static Date parseTz(String time) {
        try {
            time = fixTime(time);
            return FORMAT_TIME_ZONE.get().parse(time);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fixTime(String time) {
        if (time.endsWith("Z")) {
            time = time.substring(0, time.length() - 1) + "-0000";
        } else if (time.matches(".+[+|-]\\d+:\\d+")) {
            StringBuilder b = new StringBuilder(time);
            b.deleteCharAt(time.lastIndexOf(":"));
            time = b.toString();
        } else {
            time += TIME_ZONE;
        }
        return time;
    }

    /**
     * Adds or subtracts the corresponding time field, does not
     * perform any alignment.
     * @param count The quantity to change, can be negative.
     * @param timestamp The calendar to modify.
     * @return The timestamp parameter.
     */
    public static Calendar addDays(int count, Calendar timestamp) {
        timestamp.add(Calendar.DATE, count);
        return timestamp;
    }

    /**
     * Adds or subtracts the corresponding time field, does not
     * perform any alignment.
     * @param count The quantity to change, can be negative.
     * @param timestamp The calendar to modify.
     * @return The timestamp parameter.
     */
    public static Calendar addHours(int count, Calendar timestamp) {
        timestamp.add(Calendar.HOUR_OF_DAY, count);
        return timestamp;
    }

    /**
     * Adds or subtracts the corresponding time field, does not
     * perform any alignment.
     * @param count The quantity to change, can be negative.
     * @param timestamp The calendar to modify.
     * @return The timestamp parameter.
     */
    public static Calendar addMinutes(int count, Calendar timestamp) {
        timestamp.add(Calendar.MINUTE, count);
        return timestamp;
    }

    /**
     * Adds or subtracts the corresponding time field, does not
     * perform any alignment.
     * @param count The quantity to change, can be negative.
     * @param timestamp The calendar to modify.
     * @return The timestamp parameter.
     */
    public static Calendar addMonths(int count, Calendar timestamp) {
        timestamp.add(Calendar.MONTH, count);
        return timestamp;
    }

    /**
     * Adds or subtracts the corresponding time field, does not
     * perform any alignment.
     * @param count The quantity to change, can be negative.
     * @param timestamp The calendar to modify.
     * @return The timestamp parameter.
     */
    public static Calendar addSeconds(int count, Calendar timestamp) {
        timestamp.add(Calendar.SECOND, count);
        return timestamp;
    }

    /**
     * Adds or subtracts the corresponding time field, does not
     * perform any alignment.
     * @param count The quantity to change, can be negative.
     * @param timestamp The calendar to modify.
     * @return The timestamp parameter.
     */
    public static Calendar addWeeks(int count, Calendar timestamp) {
        return addDays(count * 7, timestamp);
    }

    /**
     * Adds or subtracts the corresponding time field, does not
     * perform any alignment.
     * @param count The quantity to change, can be negative.
     * @param timestamp The calendar to modify.
     * @return The timestamp parameter.
     */
    public static Calendar addYears(int count, Calendar timestamp) {
        timestamp.add(Calendar.YEAR, count);
        return timestamp;
    }

    /**
     * Aligns the time fields to the start of the day.
     * @param timestamp The calendar to align.
     * @return The parameter.
     */
    public static Calendar alignDay(Calendar timestamp) {
        timestamp.set(Calendar.HOUR_OF_DAY, 0);
        timestamp.set(Calendar.MINUTE, 0);
        timestamp.set(Calendar.SECOND, 0);
        timestamp.set(Calendar.MILLISECOND, 0);
        return timestamp;
    }

    /**
     * Aligns the time fields to the start of given interval.
     * @param interval The number of days in the interval to align to.
     * @param timestamp The calendar to align.
     * @return The calendar parameter, aligned.
     */
    public static Calendar alignDays(int interval, Calendar timestamp) {
        int value = timestamp.get(Calendar.DATE);
        value = value - (value % interval);
        timestamp.set(Calendar.DATE, value);
        return alignDay(timestamp);
    }

    /**
     * Aligns the time fields to the start of the hour.
     * @param timestamp The calendar to align.
     * @return The parameter.
     */
    public static Calendar alignHour(Calendar timestamp) {
        timestamp.set(Calendar.MINUTE, 0);
        timestamp.set(Calendar.SECOND, 0);
        timestamp.set(Calendar.MILLISECOND, 0);
        return timestamp;
    }

    /**
     * Aligns the time fields to the start of given interval.
     * @param interval The number of hours in the interval to align to.
     * @param timestamp The calendar to align.
     * @return The calendar parameter, aligned.
     */
    public static Calendar alignHours(int interval, Calendar timestamp) {
        int value = timestamp.get(Calendar.HOUR_OF_DAY);
        value = value - (value % interval);
        timestamp.set(Calendar.HOUR_OF_DAY, value);
        return alignHour(timestamp);
    }

    /**
     * Aligns the time fields to the start of the minute.
     * @param timestamp The calendar to align.
     * @return The parameter.
     */
    public static Calendar alignMinute(Calendar timestamp) {
        timestamp.set(Calendar.SECOND, 0);
        timestamp.set(Calendar.MILLISECOND, 0);
        return timestamp;
    }

    /**
     * Aligns the time fields to the start of given interval.
     * @param interval The number of minutes in the interval to align to.
     * @param timestamp The calendar to align.
     * @return The calendar parameter, aligned.
     */
    public static Calendar alignMinutes(int interval, Calendar timestamp) {
        int value = timestamp.get(Calendar.MINUTE);
        value = value - (value % interval);
        timestamp.set(Calendar.MINUTE, value);
        return alignMinute(timestamp);
    }

    /**
     * Aligns the time fields to the start of the month.
     * @param timestamp The calendar to align.
     * @return The parameter.
     */
    public static Calendar alignMonth(Calendar timestamp) {
        timestamp.set(Calendar.DAY_OF_MONTH, 1);
        return alignDay(timestamp);
    }

    /**
     * Aligns the time fields to the start of given interval.
     * @param interval The number of months in the interval to align to.
     * @param timestamp The calendar to align.
     * @return The calendar parameter, aligned.
     */
    public static Calendar alignMonths(int interval, Calendar timestamp) {
        int value = timestamp.get(Calendar.MONTH);
        value = value - (value % interval);
        timestamp.set(Calendar.MONTH, value);
        return alignMonth(timestamp);
    }

    /**
     * Aligns the time fields to the start of the second.
     * @param timestamp The calendar to align.
     * @return The parameter.
     */
    public static Calendar alignSecond(Calendar timestamp) {
        timestamp.set(Calendar.MILLISECOND, 0);
        return timestamp;
    }

    /**
     * Aligns the time fields to the start of given interval.
     * @param interval The number of seconds in the interval to align to.
     * @param timestamp The calendar to align.
     * @return The calendar parameter, aligned.
     */
    public static Calendar alignSeconds(int interval, Calendar timestamp) {
        int value = timestamp.get(Calendar.SECOND);
        value = value - (value % interval);
        timestamp.set(Calendar.SECOND, value);
        return alignSecond(timestamp);
    }

    /**
     * Aligns the time fields to the start of the week.
     * @param timestamp The calendar to align.
     * @return The parameter.
     */
    public static Calendar alignWeek(Calendar timestamp) {
        timestamp = alignDay(timestamp);
        int dayOfWeek = getDayOfWeek(timestamp);
        int offset = 1 - dayOfWeek;
        if (offset == 0) {
            return timestamp;
        }
        return addDays(offset, timestamp);
    }

    /**
     * Aligns the time fields to the start of the year.
     * @param timestamp The calendar to align.
     * @return The parameter.
     */
    public static Calendar alignYear(Calendar timestamp) {
        timestamp.set(Calendar.MONTH, 0);
        return alignMonth(timestamp);
    }

    /**
     * Ordinal day of the week 1=Sunday...7=Saturday.
     */
    public static int getDayOfWeek(Calendar timestamp) {
        return timestamp.get(Calendar.DAY_OF_WEEK);
    }
}
