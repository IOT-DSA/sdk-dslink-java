package org.dsa.iot.dslink.util;

import org.dsa.iot.dslink.util.log.LogLevel;
import org.dsa.iot.dslink.util.log.LogManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Samuel Grenier
 */
public class LogManagerTest {

    @Test
    public void logLevelSetting() {
        LogManager.setLevel("debug");
        Assert.assertEquals(LogLevel.DEBUG, LogManager.getLevel());

        LogManager.setLevel("info");
        Assert.assertEquals(LogLevel.INFO, LogManager.getLevel());

        LogManager.setLevel("warn");
        Assert.assertEquals(LogLevel.WARN, LogManager.getLevel());

        LogManager.setLevel("error");
        Assert.assertEquals(LogLevel.ERROR, LogManager.getLevel());

        LogManager.setLevel("none");
        Assert.assertEquals(LogLevel.OFF, LogManager.getLevel());
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
        LogManager.setLevel((LogLevel) null);
    }
}
