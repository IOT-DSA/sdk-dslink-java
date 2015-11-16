package org.dsa.iot.historian.utils;

import org.dsa.iot.dslink.util.TimeUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Samuel Grenier
 */
public class TimeParser {

    private static final ThreadLocal<DateFormat> FORMAT_TIME_ZONE;
    private static final ThreadLocal<DateFormat> FORMAT;

    public static long parse(String time) {
        try {
            time = TimeUtils.fixTime(time);
            return FORMAT_TIME_ZONE.get().parse(time).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String parse(long time) {
        return FORMAT.get().format(new Date(time)) + "-00:00";
    }

    static {
        FORMAT_TIME_ZONE = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                String pattern = TimeUtils.getTimePatternTz();
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf;
            }
        };

        FORMAT = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                String pattern = TimeUtils.getTimePattern();
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf;
            }
        };
    }
}
