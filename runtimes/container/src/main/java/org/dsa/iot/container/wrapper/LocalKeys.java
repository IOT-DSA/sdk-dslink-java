package org.dsa.iot.container.wrapper;

import java.io.File;
import java.lang.reflect.Method;

/**
 * @author Samuel Grenier
 */
public class LocalKeys {

    private Class<?> keysClass;
    private Object instance;

    public LocalKeys(ClassLoader loader) {
        try {
            String name = "org.dsa.iot.dslink.handshake.LocalKeys";
            keysClass = loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getInstance() {
        return instance;
    }

    public Class<?> getKeysClass() {
        return keysClass;
    }

    public void getFromFileSystem(File file) {
        try {
            Method generate = keysClass.getMethod("getFromFileSystem", File.class);
            instance = generate.invoke(null, file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
