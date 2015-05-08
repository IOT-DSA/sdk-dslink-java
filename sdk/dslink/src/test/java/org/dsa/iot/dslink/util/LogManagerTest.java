package org.dsa.iot.dslink.util;

import ch.qos.logback.classic.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class LogManagerTest {

    @Before
    public void setup() {
        // Coverage
        new LogManager();
    }

    @Test
    public void logLevelSetting() {
        LogManager.setLevel("debug");
        Assert.assertEquals(Level.DEBUG, LogManager.getLevel());

        LogManager.setLevel("info");
        Assert.assertEquals(Level.INFO, LogManager.getLevel());

        LogManager.setLevel("warn");
        Assert.assertEquals(Level.WARN, LogManager.getLevel());

        LogManager.setLevel("error");
        Assert.assertEquals(Level.ERROR, LogManager.getLevel());

        LogManager.setLevel("none");
        Assert.assertEquals(Level.OFF, LogManager.getLevel());
    }

    @Test(expected = RuntimeException.class)
    public void unknownLogLevel() {
        LogManager.setLevel("unknown");
    }

    @Test(expected = NullPointerException.class)
    public void nullStringLogLevel() {
        LogManager.setLevel((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullLevelLogLevel() {
        LogManager.setLevel((Level) null);
    }
}
