package org.dsa.iot.requester;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import java.util.Map;

/**
 * Responder simply lists everything from the "/" root.
 *
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private DSLink link;

    @Override
    public synchronized void onRequesterConnected(DSLink link) {
        this.link = link;
        LOGGER.info("--------------");
        LOGGER.info("Connected!");
        ListRequest request = new ListRequest("/");
        link.getRequester().list(request, new Lister(request));
        LOGGER.info("Sent data");
    }

    public static void main(String[] args) {
        DSLinkFactory.startRequester("requester", args, new Main());
    }

    private class Lister implements Handler<ListResponse> {

        private final ListRequest request;

        public Lister(ListRequest request) {
            this.request = request;
        }

        @Override
        public void handle(ListResponse resp) {
            LOGGER.info("--------------");
            LOGGER.info("Received response: " + request.getName());
            Node node = resp.getNode();
            LOGGER.info("Path: " + request.getPath());
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
                    link.getRequester().list(newReq, new Lister(newReq));
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
    }

}
