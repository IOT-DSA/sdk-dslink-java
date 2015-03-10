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
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.requester.Requester;
import org.dsa.iot.dslink.responder.Responder;
import org.vertx.java.core.json.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        init();
    }

    @SneakyThrows
    protected void init() {
        if (responder == null) {
            return;
        }

        final Path path = Paths.get("data.json");
        final Path backup = Paths.get("data.json.bak");
        { // Deserialize the data
            if (Files.exists(path)) {
                // TODO: deserialize
            } else if (Files.exists(backup)) {
                Files.copy(backup, path);
            }
            Files.deleteIfExists(backup);
        }

        { // Start save thread
            val thread = new Thread(new Runnable() {
                @Override
                @SuppressWarnings("InfiniteLoopStatement")
                public void run() {
                    while (true) {
                        try {
                            TimeUnit.SECONDS.sleep(5);
                            if (Files.exists(path)) {
                                val copyOpt = StandardCopyOption.REPLACE_EXISTING;
                                Files.copy(path, backup, copyOpt);
                                Files.delete(path);
                            }

                            val manager = getNodeManager();
                            val root = new JsonObject();
                            iterateChildren(root, manager.getNode("/").getKey());
                            Files.write(path, root.encodePrettily().getBytes("UTF-8"));
                            Files.deleteIfExists(backup);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
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

    private void iterateChildren(JsonObject out, Node parent) {
        val data = new JsonObject();

        { // Value
            val value = parent.getValue();
            if (value != null) {
                ValueUtils.toJson(data, "?value", value);
            }
        }

        { // Configurations
            val confs = parent.getConfigurations();
            if (confs != null) {
                for (Map.Entry<String, Value> conf : confs.entrySet()) {
                    val name = conf.getKey();
                    val value = conf.getValue();
                    ValueUtils.toJson(data, "$" + name, value);
                }
            }
        }

        { // Attributes
            val attribs = parent.getAttributes();
            if (attribs != null) {
                for (Map.Entry<String, Value> attr : attribs.entrySet()) {
                    val name = attr.getKey();
                    val value = attr.getValue();
                    ValueUtils.toJson(data, "@" + name, value);
                }
            }
        }

        { // Action
            val act = parent.getAction();
            if (act != null) {
                act.toJson(data);
            }
        }

        { // Children
            val children = parent.getChildren();
            if (children != null) {
                for (Node n : children.values()) {
                    iterateChildren(data, n);
                }
            }
        }

        out.putObject(parent.getName(), data);
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
