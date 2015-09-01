package org.dsa.iot.container.wrapper;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Samuel Grenier
 */
public class Configuration {

    private final Class<?> configClass;
    private Object instance;

    public Configuration(ClassLoader loader) {
        String clazz = "org.dsa.iot.dslink.config.Configuration";
        try {
            configClass = loader.loadClass(clazz);
            instance = configClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public Object getInstance() {
        return instance;
    }

    public void setConnectionType(String type) {
        ClassLoader loader = configClass.getClassLoader();
        String clazz = "org.dsa.iot.dslink.connection.ConnectionType";
        try {
            Class<?> connType = loader.loadClass(clazz);
            Field webSocket = connType.getField(type);

            Method method = configClass.getMethod("setConnectionType", connType);
            method.invoke(instance, webSocket.get(null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setAuthEndpoint(String string) {
        Method method;
        try {
            method = configClass.getMethod("setAuthEndpoint", String.class);
            method.invoke(instance, string);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setDsId(String string) {
        Method method;
        try {
            method = configClass.getMethod("setDsId", String.class);
            method.invoke(instance, string);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setRequester(boolean bool) {
        Method method;
        try {
            method = configClass.getMethod("setRequester", Boolean.TYPE);
            method.invoke(instance, bool);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setResponder(boolean bool) {
        Method method;
        try {
            method = configClass.getMethod("setResponder", Boolean.TYPE);
            method.invoke(instance, bool);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setKeys(LocalKeys keys) {
        Method method;
        try {
            method = configClass.getMethod("setKeys", keys.getKeysClass());
            method.invoke(instance, keys.getInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setSerializationPath(File path) {
        Method method;
        try {
            method = configClass.getMethod("setSerializationPath", File.class);
            method.invoke(instance, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
