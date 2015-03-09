package org.dsa.iot.dslink;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.dsa.iot.core.event.Event;
import org.dsa.iot.dslink.connection.ClientConnector;
import org.dsa.iot.dslink.connection.ServerConnector;
import org.dsa.iot.dslink.events.IncomingDataEvent;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.responder.Responder;

/**
 * @author Samuel Grenier
 */
public class DSLink {

    @Getter
    private final MBassador<Event> bus;

    private final ClientConnector clientConnector;
    private final ServerConnector serverConnector;

    @Getter
    private final Requester requester;

    @Getter
    private final Responder responder;

    protected DSLink(MBassador<Event> bus,
                    ClientConnector clientConn,
                    ServerConnector serverConn,
                    Requester req,
                    Responder resp) {
        this.bus = bus;
        this.clientConnector = clientConn;
        this.serverConnector = serverConn;
        this.requester = req;
        this.responder = resp;
        bus.subscribe(this);

        NodeManager common = new NodeManager(bus);
        if (requester != null)
            requester.setConnector(clientConn, serverConn, common);
        if (responder != null)
            responder.setConnector(clientConn, serverConn, common);

    }

    public boolean isListening() {
        return serverConnector != null && serverConnector.isListening();
    }

    public void listen(int port) {
        listen(port, "0.0.0.0");
    }

    public void listen(int port, @NonNull String bindAddr) {
        // TODO: SSL support
        checkListening();
        serverConnector.start(port, bindAddr);
    }

    public void stopListening() {
        serverConnector.stop();
    }

    public boolean isConnecting() {
        return clientConnector != null && clientConnector.isConnecting();
    }

    public boolean isConnected() {
        return clientConnector != null && clientConnector.isConnected();
    }

    public void connect() {
        connect(true);
    }

    public void connect(boolean sslVerify) {
        checkConnected();
        clientConnector.connect(sslVerify);
    }

    public void disconnect() {
        if (clientConnector.isConnected()) {
            clientConnector.disconnect();
        }
    }

    public NodeManager getNodeManager() {
        if (requester != null)
            return requester.getManager();
        else if (responder != null)
            return responder.getManager();
        else
            return null;
    }

    /**
     * Blocks the thread until the link is disconnected or the server is
     * stopped.
     */
    @SneakyThrows
    @SuppressWarnings("SleepWhileInLoop")
    public void sleep() {
        while (isConnecting() || isConnected() || isListening()) {
            Thread.sleep(500);
        }
    }

    @Handler
    public void jsonHandler(IncomingDataEvent event) {
        try {
            val data = event.getData();
            if (responder != null) {
                val array = data.getArray("requests");
                if (array != null) {
                    responder.parse(event.getClient(), array);
                }
            }
            if (requester != null) {
                val array = data.getArray("responses");
                if (array != null) {
                    requester.parse(event.getClient(), array);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasClientConnector() {
        return clientConnector != null;
    }

    private void checkConnected() {
        if (!hasClientConnector()) {
            throw new IllegalStateException("No client connector implementation provided");
        } else if (clientConnector.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
    }

    private void checkListening() {
        if (serverConnector == null) {
            throw new IllegalStateException("No server connector implementation provided");
        } else if (serverConnector.isListening()) {
            throw new IllegalStateException("Already listening");
        }
    }
}
