package org.dsa.iot.requester;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.DSLinkProvider;
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

    private DSLinkProvider provider;
    private DSLink link;

    @Override
    public boolean isRequester() {
        return true;
    }

    @Override
    public void onRequesterConnected(DSLink link) {
        this.link = link;
        LOGGER.info("--------------");
        LOGGER.info("Connected!");
        ListRequest request = new ListRequest("/");
        link.getRequester().list(request, new Lister(request));
        LOGGER.info("Sent data");
    }

    @Override
    public void onRequesterDisconnected(DSLink link) {
        LOGGER.info("Oh no! The connection to the broker was lost");
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.provider = DSLinkFactory.generate(args, main);
        if (main.provider == null) {
            return;
        }
        main.provider.start();
        main.provider.sleep();
    }

    private class Lister implements Handler<ListResponse> {

        private final ListRequest request;

        public Lister(ListRequest request) {
            this.request = request;
        }

        @Override
        public void handle(ListResponse resp) {
            String msg = "\n";
            msg += "Received response: " + request.getName() + "\n";
            msg += "Path: " + request.getPath() + "\n";
            Node node = resp.getNode();
            msg += printValueMap(node.getAttributes(), "Attribute", false);
            msg += printValueMap(node.getConfigurations(), "Configuration", false);
            {
                boolean isAction = node.getAction() != null;
                msg += "Is action? " + isAction + "\n";
            }
            {
                boolean isMetric = node.getValueType() != null;
                msg += "Is metric? " + isMetric + "\n";
            }

            Map<String, Node> nodes = node.getChildren();
            if (nodes != null) {
                msg += "Children: \n";
                for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                    String name = entry.getKey();
                    Node child = entry.getValue();
                    msg += "    Name: " + name + "\n";
                    msg += printValueMap(child.getAttributes(), "Attribute", true);
                    msg += printValueMap(child.getConfigurations(), "Configuration", true);

                    ListRequest newReq = new ListRequest(child.getPath());
                    link.getRequester().list(newReq, new Lister(newReq));
                }
            }
            LOGGER.info(msg);
            System.out.flush();
        }

        private String printValueMap(Map<String, Value> map, String name,
                                   boolean indent) {
            String msg = "";
            if (map != null) {
                for (Map.Entry<String, Value> conf : map.entrySet()) {
                    String a = conf.getKey();
                    String v = conf.getValue().toString();
                    if (indent) {
                        msg += "      ";
                    }
                    msg += name + ": " + a + " => " + v + "\n";
                }
            }
            return msg;
        }
    }

}
