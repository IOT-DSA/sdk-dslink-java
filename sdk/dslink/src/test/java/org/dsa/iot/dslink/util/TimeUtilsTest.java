package org.dsa.iot.dslink.util;

import org.junit.Test;

import java.sql.*;
import java.util.*;

/**
 * Tests for org.dsa.iot.dslink.util.TimeUtils
 */
public class TimeUtilsTest {

    /**
     * Convenience for constructing calendars.
     */
    private Calendar make(
            int year,
            int month,
            int day,
            int hour,
            int minute,
            int second) {
        Calendar ret = Calendar.getInstance();
        ret.set(year,month,day,hour,minute,second);
        return TimeUtils.alignSecond(ret);
    }

    @Test
    public void testAdding() {
        Calendar cal = make(2016,3,6,8,55,30);
        long origTs = cal.getTimeInMillis();
        //--- Seconds ---
        //positive
        TimeUtils.addSeconds(15,cal);
        validateEqual(cal,make(2016,3,6,8,55,45));
        //positive + cross boundary
        cal.setTimeInMillis(origTs); //2016,3,6,8,55,30
        TimeUtils.addSeconds(60,cal);
        validateEqual(cal,make(2016,3,6,8,56,30));
        //negative
        cal.setTimeInMillis(origTs);
        TimeUtils.addSeconds(-15,cal);
        validateEqual(cal,make(2016,3,6,8,55,15));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addSeconds(-60,cal);
        validateEqual(cal,make(2016,3,6,8,54,30));
        //--- Minutes ---
        //positive
        cal.setTimeInMillis(origTs); //2016,3,6,8,55,30
        TimeUtils.addMinutes(2,cal);
        validateEqual(cal,make(2016,3,6,8,57,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addMinutes(10,cal);
        validateEqual(cal,make(2016,3,6,9,5,30));
        //negative
        cal.setTimeInMillis(origTs);
        TimeUtils.addMinutes(-15,cal);
        validateEqual(cal,make(2016,3,6,8,40,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addMinutes(-60,cal);
        validateEqual(cal,make(2016,3,6,7,55,30));
        //--- Days ---
        //positive
        cal.setTimeInMillis(origTs); //2016,3,6,8,55,30
        TimeUtils.addDays(2,cal);
        validateEqual(cal,make(2016,3,8,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addDays(30,cal);
        validateEqual(cal,make(2016,4,6,8,55,30));
        //negative
        cal.setTimeInMillis(origTs);
        TimeUtils.addDays(-2,cal);
        validateEqual(cal,make(2016,3,4,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addDays(-31,cal);
        validateEqual(cal,make(2016,2,6,8,55,30));
        //--- Weeks ---
        //positive
        cal.setTimeInMillis(origTs); //2016,3,6,8,55,30
        TimeUtils.addWeeks(2,cal);
        validateEqual(cal,make(2016,3,20,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addWeeks(4,cal);
        validateEqual(cal,make(2016,4,4,8,55,30));
        //negative
        cal.setTimeInMillis(origTs);
        TimeUtils.addWeeks(-1,cal);
        validateEqual(cal,make(2016,2,30,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addWeeks(-4,cal);
        validateEqual(cal,make(2016,2,9,8,55,30));
        //--- Months ---
        //positive
        cal.setTimeInMillis(origTs); //2016,3,6,8,55,30
        TimeUtils.addMonths(1,cal);
        validateEqual(cal,make(2016,4,6,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addMonths(12,cal);
        validateEqual(cal,make(2017,3,6,8,55,30));
        //negative
        cal.setTimeInMillis(origTs);
        TimeUtils.addMonths(-1,cal);
        validateEqual(cal,make(2016,2,6,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addMonths(-12,cal);
        validateEqual(cal,make(2015,3,6,8,55,30));
        //--- Years ---
        //positive
        cal.setTimeInMillis(origTs); //2016,3,6,8,55,30
        TimeUtils.addYears(1,cal);
        validateEqual(cal,make(2017,3,6,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addYears(10,cal);
        validateEqual(cal,make(2026,3,6,8,55,30));
        //negative
        cal.setTimeInMillis(origTs);
        TimeUtils.addYears(-1,cal);
        validateEqual(cal,make(2015,3,6,8,55,30));
        //positive + cross boundary
        cal.setTimeInMillis(origTs);
        TimeUtils.addYears(-10,cal);
        validateEqual(cal,make(2006,3,6,8,55,30));
    }

    @Test
    public void testAlignment() {
        Calendar cal = make(2016,3,6,8,55,30);
        long origTs = cal.getTimeInMillis();
        //--- Seconds ---
        TimeUtils.alignSecond(cal);
        validateEqual(cal,make(2016,3,6,8,55,30));
        //common case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignSeconds(20,cal);
        validateEqual(cal,make(2016,3,6,8,55,20));
        //align to self
        cal.setTimeInMillis(origTs);
        TimeUtils.alignSeconds(10,cal);
        validateEqual(cal,make(2016,3,6,8,55,30));
        //odd case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignSeconds(7,cal);
        validateEqual(cal,make(2016,3,6,8,55,28));
        //--- Minutes ---
        cal.setTimeInMillis(origTs);
        TimeUtils.alignMinute(cal);
        validateEqual(cal,make(2016,3,6,8,55,0));
        //common case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignMinutes(15,cal);
        validateEqual(cal,make(2016,3,6,8,45,0));
        //align to self
        cal.setTimeInMillis(origTs);
        TimeUtils.alignMinutes(5,cal);
        validateEqual(cal,make(2016,3,6,8,55,0));
        //odd case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignMinutes(7,cal);
        validateEqual(cal,make(2016,3,6,8,49,0));
        //--- Hours ---
        cal.setTimeInMillis(origTs);
        TimeUtils.alignHour(cal);
        validateEqual(cal,make(2016,3,6,8,0,0));
        //common case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignHours(6,cal);
        validateEqual(cal,make(2016,3,6,6,0,0));
        //align to self
        cal.setTimeInMillis(origTs);
        TimeUtils.alignHours(2,cal);
        validateEqual(cal,make(2016,3,6,8,0,0));
        //odd case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignHours(3,cal);
        validateEqual(cal,make(2016,3,6,6,0,0));
        //beginning of day
        cal.setTimeInMillis(origTs);
        TimeUtils.alignHours(12,cal);
        validateEqual(cal,make(2016,3,6,0,0,0));
        //--- Days ---
        cal.setTimeInMillis(origTs);
        TimeUtils.alignDay(cal);
        validateEqual(cal,make(2016,3,6,0,0,0));
        //common case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignDays(1,cal);
        validateEqual(cal,make(2016,3,6,0,0,0));
        //align to self
        cal.setTimeInMillis(origTs);
        TimeUtils.alignDays(6,cal);
        validateEqual(cal,make(2016,3,6,0,0,0));
        //odd case
        cal.setTimeInMillis(origTs);
        TimeUtils.alignDays(5,cal);
        validateEqual(cal,make(2016,3,5,0,0,0));
        //--- Weeks ---
        cal.setTimeInMillis(origTs);
        TimeUtils.alignWeek(cal);
        validateEqual(cal,make(2016,3,3,0,0,0));
        //--- Months ---
        cal.setTimeInMillis(origTs);
        TimeUtils.alignMonth(cal);
        validateEqual(cal,make(2016,3,1,0,0,0));
        //--- Years ---
        cal.setTimeInMillis(origTs);
        TimeUtils.alignYear(cal);
        validateEqual(cal,make(2016,0,1,0,0,0));
    }

    @Test
    public void testDecoding() {
        TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar correctTime = make(2016,0,1,0,0,0);
        correctTime.setTimeZone(timeZone);
        String encoded = "2016-01-01T00:00:00.000";
        Calendar cal = TimeUtils.decode(encoded,timeZone);
        validateEqual(cal,correctTime);
        encoded = "2016-01-01T00:00:00.000-08:00";
        cal = TimeUtils.decode(encoded,null);
        cal.setTimeZone(timeZone);
        validateEqual(cal,correctTime);
        try {
            TimeUtils.decode("2016_01-01T00:00:00.000-08:00",null);
            throw new IllegalStateException();
        } catch (IllegalArgumentException ignored) {}
        try {
            TimeUtils.decode("2016-1-01T00:00:00.000-08:00",null);
            throw new IllegalStateException();
        } catch (IllegalArgumentException ignored) {}
        try {
            TimeUtils.decode("2016_01-01T0:00:00.000-08:00",null);
            throw new IllegalStateException();
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testEncoding() {
        TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar cal = make(2016,0,1,0,0,0);
        cal.setTimeZone(timeZone);
        String encoded = TimeUtils.encode(cal,false,null).toString();
        validateEqual(encoded,"2016-01-01T00:00:00.000");
        encoded = TimeUtils.encode(cal,true,null).toString();
        validateEqual(encoded,"2016-01-01T00:00:00.000-08:00");
    }

    /**
     * Throws an IllegalStateException if the two calendars are not equal.
     */
    private void validateEqual(Calendar c1, Calendar c2) {
        if (c1.getTimeInMillis() != c2.getTimeInMillis()) {
            System.out.print(TimeUtils.format(c1.getTimeInMillis()));
            System.out.print(" != ");
            System.out.println(TimeUtils.format(c2.getTimeInMillis()));
            throw new IllegalStateException();
        }
    }

    /**
     * Throws an IllegalStateException if the two Strings are not equal.
     */
    private void validateEqual(String s1, String s2) {
        if (!s1.equals(s2)) {
            System.out.print(s1);
            System.out.print(" != ");
            System.out.println(s1);
            throw new IllegalStateException();
        }
    }

}
