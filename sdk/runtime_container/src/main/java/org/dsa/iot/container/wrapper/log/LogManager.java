package org.dsa.iot.container.wrapper.log;

import java.lang.reflect.Method;

/**
 * @author Samuel Grenier
 */
public class LogManager {

    private final ClassLoader loader;

    public LogManager(ClassLoader loader) {
        if (loader == null) {
            throw new NullPointerException("loader");
        }
        this.loader = loader;
    }

    public void setLevel(String level) {
        String logManagerClass = "org.dsa.iot.dslink.util.log.LogManager";
        try {
            Class<?> managerClazz = loader.loadClass(logManagerClass);
            Method method = managerClazz.getMethod("setLevel", String.class);
            method.invoke(null, level);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
