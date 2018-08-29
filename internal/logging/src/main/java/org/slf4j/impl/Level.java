package org.slf4j.impl;

/**
 * @author Samuel Grenier
 */
public class Level {
    public static final Level OFF = new Level(0, "OFF");
    public static final Level ERROR = new Level(100, "ERROR");
    public static final Level WARN = new Level(200, "WARN");
    public static final Level INFO = new Level(300, "INFO");
    public static final Level DEBUG = new Level(400, "DEBUG");
    public static final Level TRACE = new Level(500, "TRACE");

    private final int level;
    private final String name;

    public Level(int level, String name) {
        this.level = level;
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }
}
