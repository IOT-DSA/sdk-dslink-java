package org.dsa.iot.requester;

import ch.qos.logback.classic.Level;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.Configuration;
import org.dsa.iot.dslink.util.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Responder simply lists everything from the "/" root.
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private DSLink link;

    public Main(Configuration configuration) {
        super(configuration);
    }

    @Override
    public synchronized void onConnected(DSLink link) {
        this.link = link;
        LOGGER.info("--------------");
        LOGGER.info("Connected!");
        ListRequest request = new ListRequest("/");
        link.getRequester().sendRequest(request);
        LOGGER.info("Sent data");
    }

    @Override
    public synchronized void onListResponse(ListRequest req, ListResponse resp) {
        LOGGER.info("--------------");
        LOGGER.info("Received response: " + req.getName());
        Node node = resp.getNode();
        LOGGER.info("Path: " + req.getPath());
        printValueMap(node.getAttributes(), "Attribute", false);
        printValueMap(node.getConfigurations(), "Configuration", false);
        Map<String, Node> nodes = node.getChildren();
        if (nodes != null) {
            LOGGER.info("Children: ");
            for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                String name = entry.getKey();
                Node child = entry.getValue();
                LOGGER.info("    Name: " + name);

                printValueMap(child.getAttributes(), "Attribute", true);
                printValueMap(child.getConfigurations(), "Configuration", true);

                ListRequest newReq = new ListRequest(child.getPath());
                link.getRequester().sendRequest(newReq);
            }
        }
    }

    private void printValueMap(Map<String, Value> map, String name,
                               boolean indent) {
        if (map != null) {
            for (Map.Entry<String, Value> conf : map.entrySet()) {
                String a = conf.getKey();
                String v = conf.getValue().toDebugString();
                if (indent) {
                    LOGGER.info("      ");
                }
                LOGGER.info(name + ": " + a + " => " + v);
            }
        }
    }

    public static void main(String[] args) {
        LogLevel.setLevel(Level.INFO);
        Configuration config = new Configuration();
        config.setDsId("requester");
        config.setKeys(LocalKeys.generate());
        config.setConnectionType(ConnectionType.WEB_SOCKET);
        config.setAuthEndpoint("http://localhost:8080/conn");
        config.setRequester(true);

        Main main = new Main(config);
        DSLink link = DSLinkFactory.generate(main);
        link.start();
        link.sleep();
    }
}
