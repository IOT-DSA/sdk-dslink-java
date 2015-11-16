package org.dsa.iot.dslink.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private static final String TIME_ZONE;

    public static String getTimePatternTz() {
        return TIME_PATTERN_TZ;
    }

    public static String getTimePattern() {
        return TIME_PATTERN;
    }

    public static String getTimeZone() {
        return TIME_ZONE;
    }

    public static String format(Date time) {
        return FORMAT.get().format(time) + TimeUtils.getTimeZone();
    }

    public static Date parseTz(String time) {
        try {
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
        TIME_ZONE = s + (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
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
}
