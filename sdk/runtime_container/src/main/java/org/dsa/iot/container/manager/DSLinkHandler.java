package org.dsa.iot.container.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.dsa.iot.container.security.SecMan;
import org.dsa.iot.container.wrapper.DSLinkProvider;
import org.dsa.iot.container.wrapper.log.LogManager;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author Samuel Grenier
 */
public class DSLinkHandler {

    private final DSLinkInfo info;

    private DSLinkProvider provider;
    private ClassLoader loader;

    public DSLinkHandler(DSLinkInfo info) {
        this.info = info;
    }

    public synchronized void start() throws IOException {
        if (isRunning()) {
            return;
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    URL[] urls = info.collectJars();
                    loader = new URLClassLoader(urls);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });

        LogManager manager = new LogManager(loader);
        manager.setLevel(info.getLogLevel());

        provider = new DSLinkProvider(loader, info);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Set security manager per dslink thread
                System.setSecurityManager(new SecMan());
                provider.start();
                provider.sleep();
            }
        }, info.getName());
        thread.start();

        // TODO: stop vert.x and thread pools
    }

    @SuppressFBWarnings("DM_GC")
    public synchronized void stop() {
        if (isRunning()) {
            provider.stop();
            provider = null;
            loader = null;
            System.gc();
        }
    }

    public synchronized boolean isRunning() {
        return loader != null;
    }
}
