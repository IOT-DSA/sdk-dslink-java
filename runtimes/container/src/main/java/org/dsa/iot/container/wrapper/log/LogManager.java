package org.dsa.iot.container.wrapper.log;

import java.io.File;
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

    public void configure(String logPath) {
        try {
            if (logPath != null) {
                Class<?> managerClass = getManagerClass();
                Method method = managerClass.getMethod("configure", File.class);
                method.invoke(null, new File(logPath));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setLevel(String level) {
        try {
            Class<?> managerClass = getManagerClass();
            Method method = managerClass.getMethod("setLevel", String.class);
            method.invoke(null, level);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> getManagerClass() {
        String logManagerClass = "org.dsa.iot.dslink.util.log.LogManager";
        try {
            return loader.loadClass(logManagerClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
