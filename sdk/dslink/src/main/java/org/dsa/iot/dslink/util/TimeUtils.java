package org.dsa.iot.dslink.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Samuel Grenier
 */
public class TimeUtils {

    public static final int MILLIS_MINUTE = 60 * 1000;
    public static final int MILLIS_HOUR = 60 * MILLIS_MINUTE;

    private static final ThreadLocal<DateFormat> FORMAT_TIME_ZONE;
    private static final ThreadLocal<DateFormat> FORMAT;
    private static final String TIME_PATTERN_TZ;
    private static final String TIME_PATTERN;
    private static final String TIME_ZONE_COLON;
    private static final String TIME_ZONE;

    private static final Map<String,TimeZone> timezones = new HashMap<String,TimeZone>();

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
        int dayOfWeek = timestamp.get(Calendar.DAY_OF_WEEK);
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
     * Converts the characters into an int.
     */
    private static int convertDigits(char tens, char ones) {
        return (toDigit(tens) * 10) + toDigit(ones);
    }

    /**
     * Converts the characters into an int.
     */
    private static int convertDigits(char thousands, char hundreds, char tens, char ones) {
        return toDigit(thousands) * 1000 +
               toDigit(hundreds) * 100 +
               toDigit(tens) * 10 +
               toDigit(ones);
    }

    /**
     * Converts a DSA encoded timestamp into a Java Calendar.  DSA encoding is based
     * on ISO 8601 but allows for an unspecified timezone.
     * @param timestamp The encoded timestamp.
     * @param calendar The instance to decode into and returnt, may be null.  If the timestamp does
     *                 not specify a timezone, the zone in this instance will be used.
     */
    public static Calendar decode(String timestamp, Calendar calendar) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        try {
            char[] chars = timestamp.toCharArray();
            int idx = 0;
            int year = convertDigits(chars[idx++], chars[idx++], chars[idx++], chars[idx++]);
            validateChar(chars[idx++], '-');
            int month = convertDigits(chars[idx++], chars[idx++]) - 1;
            validateChar(chars[idx++], '-');
            int day = convertDigits(chars[idx++], chars[idx++]);
            validateChar(chars[idx++], 'T');
            int hour = convertDigits(chars[idx++], chars[idx++]);
            validateChar(chars[idx++], ':');
            int minute = convertDigits(chars[idx++], chars[idx++]);
            validateChar(chars[idx++], ':');
            int second = convertDigits(chars[idx++], chars[idx++]);
            int millis = 0;
            if ((chars.length > idx) && (chars[idx] == '.')) {
                idx++;
                millis = convertDigits('0',chars[idx++],chars[idx++],chars[idx++]);
            }
            // timezone offset sign
            if (idx < chars.length) {
                char sign = chars[idx++];
                if (sign == 'Z') {
                    calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
                } else {
                    int tzOff;
                    if (sign != '+' && sign != '-')
                        throw new Exception();
                    int hrOff = convertDigits(chars[idx++],chars[idx++]);
                    int minOff = 0;
                    //minutes are optional in 8601
                    if (idx < chars.length) { //minutes optional
                        validateChar(chars[idx++], ':');
                        minOff = convertDigits(chars[idx++], chars[idx++]);
                    }
                    tzOff = (hrOff * MILLIS_HOUR) + (minOff * MILLIS_MINUTE);
                    if (sign == '-') {
                        tzOff *= -1;
                    }
                    TimeZone timezone = calendar.getTimeZone();
                    int localOffset = timezone.getOffset(calendar.getTimeInMillis());
                    if (localOffset != tzOff) {
                        String timeZoneName = "Offset" + tzOff;
                        synchronized (timezones) {
                            timezone = timezones.get(timeZoneName);
                            if (timezone == null) {
                                timezone = new SimpleTimeZone(tzOff, timeZoneName);
                                timezones.put(timeZoneName, timezone);
                            }
                        }
                        calendar.setTimeZone(timezone);
                    }
                }
            }
            calendar.set(year, month, day, hour, minute, second);
            calendar.set(Calendar.MILLISECOND, millis);
        } catch (Exception x) {
            throw new IllegalArgumentException("Invalid timestamp: " + timestamp);
        }
        return calendar;
    }

    /**
     * Converts a Java Calendar into a DSA encoded timestamp.  DSA encoding is based
     * on ISO 8601 but allows the timezone offset to not be specified.
     * @param calendar The calendar representing the timestamp to encode.
     * @param encodeTzOffset Whether or not to encode the timezone offset.
     * @param buf The buffer to append the encoded timestamp and return, can be null.
     * @return The buf argument, or if that was null, a new StringBuilder.
     */
    public static StringBuilder encode(
            Calendar calendar, boolean encodeTzOffset, StringBuilder buf) {
        if (buf == null) {
            buf = new StringBuilder();
        }
        long millis = calendar.getTimeInMillis();
        int tmp = calendar.get(Calendar.YEAR);
        buf.append( tmp ).append('-');
        //month
        tmp = calendar.get(Calendar.MONTH) + 1;
        if (tmp < 10) buf.append('0');
        buf.append( tmp ).append( '-' );
        //date
        tmp = calendar.get(Calendar.DAY_OF_MONTH);
        if (tmp < 10) buf.append('0');
        buf.append( tmp ).append( 'T' );
        //hour
        tmp = calendar.get(Calendar.HOUR_OF_DAY);
        if (tmp < 10) buf.append('0');
        buf.append( tmp ).append( ':' );
        //minute
        tmp = calendar.get(Calendar.MINUTE);
        if (tmp < 10) buf.append('0');
        buf.append( tmp ).append( ':' );
        //second
        tmp = calendar.get(Calendar.SECOND);
        if (tmp < 10) buf.append('0');
        buf.append( tmp ).append( '.' );
        //millis
        tmp = calendar.get(Calendar.MILLISECOND);
        if (tmp < 10) buf.append('0');
        if (tmp < 100) buf.append('0');
        buf.append( tmp );
        if (encodeTzOffset) {
            int offset = calendar.getTimeZone().getOffset(millis);
            if (offset == 0) {
                buf.append('Z');
            }
            else {
                int hrOff = Math.abs(offset / MILLIS_HOUR);
                int minOff = Math.abs((offset % MILLIS_HOUR) / MILLIS_MINUTE);
                if (offset < 0) buf.append('-');
                else buf.append('+');
                if (hrOff < 10) buf.append('0');
                buf.append(hrOff);
                buf.append(':');
                if (minOff < 10) buf.append('0');
                buf.append(minOff);
            }
        }
        return buf;
    }

    /**
     * Converts the character to a digit, throws an IllegalStateException if it isn't a
     * valid digit.
     */
    private static int toDigit(char ch) {
       if (('0' <= ch) && (ch <= '9')) {
            return ch - '0';
        }
        throw new IllegalStateException();
    }

    /**
     * Used for decoding timestamp, throws an IllegalStateException if the two characters are
     * not equal.
     */
    private static void validateChar(char c1, char c2) {
        if (c1 != c2)
            throw new IllegalStateException();
    }


}
