package org.dsa.iot.container.wrapper;

import org.dsa.iot.container.manager.DSLinkInfo;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author Samuel Grenier
 */
public class DSLinkProvider {

    private final ClassLoader loader;
    private final DSLinkInfo info;

    private Class<?> providerClass;
    private Object instance;

    public DSLinkProvider(ClassLoader loader, DSLinkInfo info) {
        this.loader = loader;
        this.info = info;
    }

    public void start() {
        Configuration config = new Configuration(loader);
        config.setConnectionType("WEB_SOCKET");
        config.setAuthEndpoint(info.getBrokerUrl());
        config.setDsId(info.getName());
        {
            LocalKeys keys = new LocalKeys(loader);
            keys.getFromFileSystem(info.getRoot().resolve(".key").toFile());
            config.setKeys(keys);
        }
        {
            File file = info.getRoot().resolve("nodes.json").toFile();
            config.setSerializationPath(file);
        }

        try {
            Class<?> clazz = loader.loadClass(info.getHandlerClass());
            Object handler = clazz.newInstance();

            Method m = clazz.getMethod("isResponder");
            config.setResponder((Boolean) m.invoke(handler));

            m = clazz.getMethod("isRequester");
            config.setRequester((Boolean) m.invoke(handler));

            m = clazz.getMethod("preInit");
            m.invoke(handler);

            generateProvider(handler, config);
            m = providerClass.getMethod("start");
            m.invoke(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() throws Exception {
        Method method = providerClass.getMethod("stop");
        method.invoke(instance);
    }

    public void sleep() {
        try {
            Method method = providerClass.getMethod("sleep");
            method.invoke(instance);
        } catch (Exception ignored) {
        }
    }

    private void generateProvider(Object handler,
                                  Configuration config) throws Exception {
        final Class<?> clazz = config.getConfigClass();

        String name = "org.dsa.iot.dslink.handshake.LocalHandshake";
        Class<?> handshakeClazz = loader.loadClass(name);
        Constructor<?> handshakeConn = handshakeClazz.getConstructor(clazz);
        Object handshake = handshakeConn.newInstance(config.getInstance());

        name = "org.dsa.iot.dslink.connection.ConnectionManager";
        Class<?> managerClazz = loader.loadClass(name);
        Constructor<?> managerConn = managerClazz.getConstructor(clazz, handshakeClazz);
        Object manager = managerConn.newInstance(config.getInstance(), handshake);

        name = "org.dsa.iot.dslink.DSLinkHandler";
        Class<?> handlerClazz = loader.loadClass(name);
        Method method = handlerClazz.getMethod("setConfig", clazz);
        method.invoke(handler, config.getInstance());

        name = "org.dsa.iot.dslink.DSLinkProvider";
        providerClass = loader.loadClass(name);
        Constructor<?> providerConn = providerClass.getConstructor(managerClazz, handlerClazz);
        instance = providerConn.newInstance(manager, handler);
    }
}
