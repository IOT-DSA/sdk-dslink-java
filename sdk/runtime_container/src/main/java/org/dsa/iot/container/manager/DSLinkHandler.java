package org.dsa.iot.container.manager;

import org.dsa.iot.container.manager.DSLinkInfo;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Samuel Grenier
 */
public class DSLinkHandler {

    private final DSLinkInfo info;
    private ClassLoader loader;

    public DSLinkHandler(DSLinkInfo info) {
        this.info = info;
    }

    public synchronized void start() throws IOException {
        if (isRunning()) {
            return;
        }

        URL[] urls = info.collectJars();
        loader = new URLClassLoader(urls);

        // TODO: get a provider
    }

    public synchronized void stop() {
        loader = null;
        // TODO: stop the provider
        System.gc();
    }

    public synchronized boolean isRunning() {
        return loader != null;
    }
}
