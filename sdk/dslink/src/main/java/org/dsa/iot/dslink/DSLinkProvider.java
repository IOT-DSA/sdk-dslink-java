package org.dsa.iot.dslink;

import org.dsa.iot.dslink.connection.ConnectionManager;
import org.dsa.iot.dslink.connection.DataHandler;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.serializer.SerializationManager;
import org.vertx.java.core.Handler;

import java.io.File;

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
        manager.start(new Handler<ClientConnected>() {
            @Override
            public synchronized void handle(ClientConnected event) {
                DataHandler h = event.getHandler();
                if (event.isRequester()) {
                    DSLink link = new DSLink(handler, h, true, true);
                    link.setDefaultDataHandlers(true, false);
                    handler.onRequesterConnected(link);
                }

                if (event.isResponder()) {
                    DSLink link = new DSLink(handler, h, false, true);

                    File path = handler.getConfig().getSerializationPath();
                    if (path != null) {
                        NodeManager man = link.getNodeManager();
                        SerializationManager manager = new SerializationManager(path, man);
                        manager.deserialize();
                        manager.start();
                    }

                    link.setDefaultDataHandlers(false, true);
                    handler.onResponderConnected(link);
                }
            }
        });
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
