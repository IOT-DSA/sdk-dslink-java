package org.dsa.iot.dslink.util;

import ch.qos.logback.classic.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class LogLevelTest {

    @Before
    public void setup() {
        // Coverage
        new LogLevel();
    }

    @Test
    public void logLevelSetting() {
        LogLevel.setLevel("debug");
        Assert.assertEquals(Level.DEBUG, LogLevel.getLevel());

        LogLevel.setLevel("info");
        Assert.assertEquals(Level.INFO, LogLevel.getLevel());

        LogLevel.setLevel("warn");
        Assert.assertEquals(Level.WARN, LogLevel.getLevel());

        LogLevel.setLevel("error");
        Assert.assertEquals(Level.ERROR, LogLevel.getLevel());

        LogLevel.setLevel("none");
        Assert.assertEquals(Level.OFF, LogLevel.getLevel());
    }

    @Test(expected = RuntimeException.class)
    public void unknownLogLevel() {
        LogLevel.setLevel("unknown");
    }

    @Test(expected = NullPointerException.class)
    public void nullStringLogLevel() {
        LogLevel.setLevel((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullLevelLogLevel() {
        LogLevel.setLevel((Level) null);
    }
}
