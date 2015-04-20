package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.ConnectionManager;
import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.serializer.SerializationManager;
import org.dsa.iot.dslink.util.Objects;
import org.vertx.java.core.Handler;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static org.dsa.iot.dslink.connection.ConnectionManager.ClientConnected;

/**
 * Provides DSLinks as soon as a client connects to the server or vice versa.
 * @author Samuel Grenier
 */
public class DSLinkProvider {

    private final ConnectionManager manager;
    private final DSLinkHandler handler;
    private boolean running;

    public DSLinkProvider(ConnectionManager manager, DSLinkHandler handler) {
        if (manager == null)
            throw new NullPointerException("manager");
        else if (handler == null)
            throw new NullPointerException("handler");
        this.manager = manager;
        this.handler = handler;
        handler.preInit();
    }

    public void start() {
        running = true;

        manager.setPreInitHandler(new Handler<ClientConnected>() {
            @Override
            public void handle(final ClientConnected event) {
                final CountDownLatch latch = new CountDownLatch(2);
                final DataHandler h = event.getHandler();
                Objects.getDaemonThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (event.isRequester()) {
                            final DSLink link = new DSLink(handler, h, true, true);
                            link.setDefaultDataHandlers(true, false);
                            handler.onRequesterInitialized(link);
                            event.setRequesterOnConnected(new Handler<ClientConnected>() {
                                @Override
                                public void handle(ClientConnected event) {
                                    handler.onRequesterConnected(link);
                                }
                            });
                        }
                        latch.countDown();
                    }
                });

                Objects.getDaemonThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (event.isResponder()) {
                            final DSLink link = new DSLink(handler, h, false, true);

                            File path = handler.getConfig().getSerializationPath();
                            if (path != null) {
                                NodeManager man = link.getNodeManager();
                                SerializationManager manager = new SerializationManager(path, man);
                                manager.deserialize();
                                manager.start();
                            }

                            link.setDefaultDataHandlers(false, true);
                            handler.onResponderInitialized(link);
                            event.setResponderOnConnected(new Handler<ClientConnected>() {
                                @Override
                                public void handle(ClientConnected event) {
                                    handler.onResponderConnected(link);
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

    public void stop() {
        running = false;
        manager.stop();
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
            throw new RuntimeException(e);
        }
    }
}
