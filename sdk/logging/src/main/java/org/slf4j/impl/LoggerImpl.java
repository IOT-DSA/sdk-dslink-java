package org.slf4j.impl;

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Samuel Grenier
 */
public class LoggerImpl implements Logger {

    private static final Pattern PATTERN = Pattern.compile("\\{\\}");
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat SDF;

    private final LoggerFactoryImpl factory;
    private final String name;

    public LoggerImpl(LoggerFactoryImpl factory, String name) {
        this.factory = factory;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return factory.getLogLevel().getLevel() >= Level.TRACE.getLevel();
    }

    @Override
    public void trace(String msg) {
        log(Level.TRACE, msg, null, null);
    }

    @Override
    public void trace(String format, Object arg) {
        log(Level.TRACE, format, new Object[] { arg }, null);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log(Level.TRACE, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void trace(String format, Object... argArray) {
        log(Level.TRACE, format, argArray, null);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(Level.TRACE, msg, null, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String msg) {
        trace((String) null, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        trace((String) null, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        trace((String) null, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray) {
        trace((String) null, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        trace((String) null, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return factory.getLogLevel().getLevel() >= Level.DEBUG.getLevel();
    }

    @Override
    public void debug(String msg) {
        log(Level.DEBUG, msg, null, null);
    }

    @Override
    public void debug(String format, Object arg) {
        log(Level.DEBUG, format, new Object[] { arg }, null);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log(Level.DEBUG, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void debug(String format, Object... argArray) {
        log(Level.DEBUG, format, argArray, null);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(Level.DEBUG, msg, null, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        debug((String) null, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        debug((String) null, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        debug((String) null, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        debug((String) null, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        debug((String) null, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return factory.getLogLevel().getLevel() >= Level.INFO.getLevel();
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg, null, null);
    }

    @Override
    public void info(String format, Object arg) {
        log(Level.INFO, format, new Object[] { arg }, null);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(Level.INFO, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void info(String format, Object... argArray) {
        log(Level.INFO, format, argArray, null);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(Level.INFO, msg, null, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String msg) {
        info((String) null, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        info((String) null, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        info((String) null, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        info((String) null, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        info((String) null, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return factory.getLogLevel().getLevel() >= Level.WARN.getLevel();
    }

    @Override
    public void warn(String msg) {
        log(Level.WARN, msg, null, null);
    }

    @Override
    public void warn(String format, Object arg) {
        log(Level.WARN, format, new Object[] { arg }, null);
    }

    @Override
    public void warn(String format, Object... argArray) {
        log(Level.WARN, format, argArray, null);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(Level.WARN, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(Level.WARN, msg, null, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String msg) {
        warn((String) null, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warn((String) null, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warn((String) null, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        warn((String) null, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        warn((String) null, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return factory.getLogLevel().getLevel() >= Level.ERROR.getLevel();
    }

    @Override
    public void error(String msg) {
        log(Level.ERROR, msg, null, null);
    }

    @Override
    public void error(String format, Object arg) {
        log(Level.ERROR, format, new Object[] { arg }, null);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(Level.ERROR, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void error(String format, Object... argArray) {
        log(Level.ERROR, format, argArray, null);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(Level.ERROR, msg, null, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String msg) {
        error((String) null, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        error((String) null, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        error((String) null, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        error((String) null, format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        error((String) null, msg, t);
    }

    private void log(Level level, String msg, Object[] args, Throwable t) {
        if (factory.getLogLevel().getLevel() < level.getLevel()) {
            return;
        }
        if (args != null) {
            Matcher matcher = PATTERN.matcher(msg);
            StringBuffer sb = new StringBuffer();
            int index = 0;
            while (matcher.find()) {
                if (args.length > index) {
                    Object obj = args[index];
                    String string;
                    if (obj instanceof String) {
                        string = (String) obj;
                    } else if (obj instanceof Throwable) {
                        StringWriter writer = new StringWriter();
                        t.printStackTrace(new PrintWriter(writer));
                        string = writer.toString();
                    } else if (obj != null) {
                        string = obj.toString();
                    } else {
                        string = "null";
                    }
                    string = Matcher.quoteReplacement(string);
                    matcher.appendReplacement(sb, string);
                } else {
                    break;
                }
                index++;
            }
            matcher.appendTail(sb);
            msg = sb.toString();
        }
        if (t != null) {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            msg += "\n" + writer.toString();
        }

        String formatted;
        Date date = new Date();
        synchronized (LOCK) {
            formatted = SDF.format(date);
        }

        formatted += " [" + Thread.currentThread().getName() + "] "
                + level.getName()
                + " " + getName()
                + " - " + msg;
        System.out.println(formatted);
    }

    static {
        SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }
}
