package org.dsa.iot.dslink;

import static org.dsa.iot.dslink.connection.ConnectionManager.Client;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.dsa.iot.dslink.connection.ConnectionManager;
import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.link.Responder;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.serializer.SerializationManager;
import org.dsa.iot.dslink.util.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides DSLinks as soon as a client connects to the server or vice versa.
 *
 * @author Samuel Grenier
 */
public class DSLinkProvider {

    private static final Logger LOGGER;

    private final Map<String, DSLink> linkRequesterCache;
    private final Map<String, DSLink> linkResponderCache;
    private final ConnectionManager manager;
    private final DSLinkHandler handler;
    private final Object lock;
    private boolean running;

    public DSLinkProvider(ConnectionManager manager, DSLinkHandler handler) {
        if (manager == null) {
            throw new NullPointerException("manager");
        } else if (handler == null) {
            throw new NullPointerException("handler");
        }
        handler.setProvider(this);
        this.linkRequesterCache = new ConcurrentHashMap<>();
        this.linkResponderCache = new ConcurrentHashMap<>();
        this.lock = new Object();
        this.manager = manager;
        this.handler = handler;
        handler.preInit();
    }

    public Map<String, DSLink> getRequesters() {
        return Collections.unmodifiableMap(linkRequesterCache);
    }

    @SuppressWarnings("unused")
    public Map<String, DSLink> getResponders() {
        return Collections.unmodifiableMap(linkResponderCache);
    }

    /**
     * Blocks the thread indefinitely while the endpoint is active or connected
     * to a host. This will automatically unblock when the endpoint becomes
     * inactive or disconnects, allowing the thread to proceed execution. Typical
     * usage is to call {@code sleep} in the main thread to prevent the application
     * from terminating abnormally.
     */
    public void sleep() {
        try {
            while (running) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            if (running) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() {
        running = true;

        final String dsId = handler.getConfig().getDsIdWithHash();
        manager.setPreInitHandler(new Handler<Client>() {
            @Override
            public void handle(final Client event) {
                final CountDownLatch latch = new CountDownLatch(2);
                final DataHandler writer = event.getHandler();
                final String path = event.getPath();
                LoopProvider.getProvider().schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (event.isRequester()) {
                            final DSLink link;
                            synchronized (lock) {
                                DSLink tmp = linkRequesterCache.get(dsId);
                                if (tmp == null) {
                                    tmp = handler.createRequesterLink(path);
                                    tmp.setWriter(writer);
                                    tmp.setDefaultDataHandlers(true, false);
                                    handler.onRequesterInitialized(tmp);
                                    linkRequesterCache.put(dsId, tmp);
                                }
                                link = tmp;
                            }

                            event.setRequesterOnConnected(new Handler<Client>() {
                                @Override
                                public void handle(Client event) {
                                    handler.onRequesterConnected(link);
                                }
                            });
                            event.setRequesterOnDisconnected(new Handler<Void>() {
                                @Override
                                public void handle(Void event) {
                                    link.getRequester().clearSubscriptions();
                                    handler.onRequesterDisconnected(link);
                                }
                            });
                        }
                        latch.countDown();
                    }
                });

                LoopProvider.getProvider().schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (event.isResponder()) {
                            final DSLink link;

                            synchronized (lock) {
                                DSLink tmp = linkResponderCache.get(dsId);
                                if (tmp == null) {
                                    tmp = handler.createResponderLink(path);
                                    tmp.setWriter(writer);
                                    File path = handler.getConfig().getSerializationPath();
                                    if (path != null) {
                                        NodeManager man = tmp.getNodeManager();
                                        SerializationManager manager;
                                        manager = new SerializationManager(path, man);
                                        try {
                                            manager.deserialize();
                                        } catch (Exception e) {
                                            LOGGER.error("Failed to deserialize nodes", e);
                                        }
                                        manager.markChangedOverride(false);
                                        manager.start();
                                        tmp.setSerialManager(manager);
                                    }

                                    tmp.setDefaultDataHandlers(false, true);
                                    handler.onResponderInitialized(tmp);
                                    linkResponderCache.put(dsId, tmp);
                                }
                                link = tmp;
                            }

                            event.setResponderOnConnected(new Handler<Client>() {
                                @Override
                                public void handle(Client event) {
                                    Responder resp = link.getResponder();
                                    resp.getSubscriptionManager().onConnected();
                                    handler.onResponderConnected(link);
                                }
                            });
                            event.setResponderOnDisconnected(new Handler<Void>() {
                                @Override
                                public void handle(Void event) {
                                    Responder resp = link.getResponder();
                                    resp.getSubscriptionManager().onDisconnected();
                                    handler.onResponderDisconnected(link);
                                }
                            });
                        }
                        latch.countDown();
                    }
                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        manager.start(null);
    }

    @SuppressWarnings("unused")
    public void stop() {
        running = false;
        manager.stop();
        handler.stop();
        for (DSLink link : linkRequesterCache.values()) {
            link.stop();
        }
        for (DSLink link : linkResponderCache.values()) {
            link.stop();
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(DSLinkProvider.class);
    }
}
