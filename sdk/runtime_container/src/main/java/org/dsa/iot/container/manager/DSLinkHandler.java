package org.dsa.iot.container.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private ThreadGroup group;
    private Thread thread;

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
        group = new ThreadGroup(info.getName());
        group.setMaxPriority(Thread.NORM_PRIORITY);
        thread = new Thread(group, new Runnable() {
            @Override
            public void run() {
                provider.start();
                provider.sleep();
            }
        }, info.getName());
        thread.start();
    }

    @SuppressFBWarnings("DM_GC")
    @SuppressWarnings("deprecation")
    public synchronized void stop() {
        if (!isRunning()) {
            return;
        }
        try {
            provider.stop();
        } catch (Throwable ignored) {
        }
        try {
            try {
                thread.interrupt();
            } catch (Throwable ignored) {
            }
            try {
                group.interrupt();
            } catch (Throwable ignored) {
            }

            Thread[] threads = new Thread[group.activeCount()];
            int count = group.enumerate(threads);
            for (int i = 0; i < count; ++i) {
                Thread t = threads[i];
                try {
                    t.interrupt();
                } catch (Throwable ignored) {
                }

                try {
                    t.stop();
                } catch (Throwable ignored) {
                }
            }

            group.destroy();
        } catch (Throwable ignored) {
        } finally {
            thread = null;
            group = null;
        }
        provider = null;
        loader = null;
        System.gc();
    }

    public synchronized boolean isRunning() {
        return loader != null;
    }
}
