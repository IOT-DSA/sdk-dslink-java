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
    private static final String TIME_ZONE;

    public static String format(long time) {
        return FORMAT.get().format(time) + TIME_ZONE;
    }

    public static String fix(String time) {
        if (time.matches(".+[+|-]\\d+:\\d+")) {
            StringBuilder builder = new StringBuilder(time);
            builder.deleteCharAt(time.lastIndexOf(":"));
            time = builder.toString();
        } else {
            time += TIME_ZONE;
        }
        return time;
    }

    public static Date parse(String time) {
        time = fix(time);
        try {
            return FORMAT_TIME_ZONE.get().parse(time);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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
        FORMAT = new ThreadLocal<DateFormat>() {
            @Override
            public DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            }
        };
        FORMAT_TIME_ZONE = new ThreadLocal<DateFormat>() {
            @Override
            public DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            }
        };
    }
}
