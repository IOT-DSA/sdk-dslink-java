package org.dsa.iot.broker;

import org.dsa.iot.broker.client.ClientManager;
import org.dsa.iot.broker.config.Arguments;
import org.dsa.iot.broker.config.broker.BrokerConfig;
import org.dsa.iot.broker.config.broker.BrokerFileConfig;
import org.dsa.iot.broker.config.broker.BrokerMemoryConfig;
import org.dsa.iot.broker.node.BrokerTree;
import org.dsa.iot.broker.server.ServerManager;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.dslink.util.log.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class Broker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Broker.class);
    private final ClientManager clients;
    private final BrokerConfig config;
    private final BrokerTree tree;

    private ServerManager server;
    private String downstreamName;

    public Broker(BrokerConfig config,
                  ClientManager clients,
                  BrokerTree tree) {
        if (config == null) {
            throw new NullPointerException("config");
        } else if (clients == null) {
            throw new NullPointerException("clients");
        } else if (tree == null) {
            throw new NullPointerException("tree");
        }
        this.clients = clients;
        this.config = config;
        this.tree = tree;
        config.readAndUpdate();
    }

    /**
     * Starts the broker.
     *
     * @see BrokerMemoryConfig For the default configurations.
     */
    public void start() {
        stop();
        try {
            LOGGER.info("Broker is starting");
            JsonObject serverConf = config.get().get("server");
            server = new ServerManager(this, serverConf);
            server.start();
        } catch (Exception e) {
            stop();
        }
    }

    /**
     * Shuts down the broker.
     */
    public void stop() {
        if (server != null) {
            LOGGER.info("Broker is shutting down");
            server.stop();
        }
    }

    public ClientManager getClientManager() {
        return clients;
    }

    public BrokerTree getTree() {
        return tree;
    }

    public String getDownstreamName() {
        if (downstreamName != null) {
            return downstreamName;
        }
        JsonObject broker = config.get().get("broker");
        downstreamName = broker.get("downstreamName");
        if (downstreamName == null
                || downstreamName.isEmpty()
                || downstreamName.contains("/")) {
            stop(); // Bad broker config, stop it immediately
            String err = "Bad downstream name: " + downstreamName;
            throw new IllegalStateException(err);
        }
        return getDownstreamName();
    }

    protected void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }, "Broker-Shutdown-Hook"));
    }

    /**
     * Creates a standard broker with unmodified functionality.
     *
     * @param args Arguments of the program.
     * @return A broker instance.
     */
    public static Broker create(String[] args) {
        Arguments parsed = new Arguments();
        if (!parsed.parse(args)) {
            return null;
        }

        if (!parsed.runServer()) {
            parsed.displayHelp();
            return null;
        }

        String log = parsed.getLogLevel();
        LogManager.setLevel(log);

        BrokerConfig conf = new BrokerFileConfig();
        ClientManager cm = new ClientManager();
        BrokerTree bt = new BrokerTree();

        Broker b = new Broker(conf, cm, bt);
        bt.initialize(b.getDownstreamName());
        b.addShutdownHook();
        return b;
    }
}
