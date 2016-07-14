package org.dsa.iot.historian.utils;

import java.util.Calendar;

public class TimestampRange {
    public final Calendar from;
    public final Calendar to;

    public TimestampRange(Calendar from, Calendar to) {
        this.from = from;
        this.to = to;
    }
}
