package org.dsa.iot.dual;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.methods.requests.InvokeRequest;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.InvokeResponse;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;
import java.util.Random;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Random RANDOM = new Random();
    private String connName;

    @Override
    public void preInit() {
        // Assumes no conflicting ID on broker
        LocalKeys keys = getConfig().getKeys();
        String dsId = getConfig().getDsId() + "-";
        dsId += keys.encodedHashPublicKey().charAt(0);
        connName = dsId;
    }

    @Override
    public void onRequesterConnected(final DSLink link) {
        LOGGER.info("Requester link added");
        link.getRequester().list(new ListRequest("/"),
                new Handler<ListResponse>() {
                    @Override
                    public void handle(ListResponse event) {
                        LOGGER.info("Request on root node complete");
                        Map<String, Node> children = event.getNode().getChildren();
                        if (children != null) {
                            for (String child : children.keySet()) {
                                LOGGER.info("Child node of root: " + child);
                            }
                        }
                    }
                });

        link.getRequester().list(new ListRequest("/conns/" + connName + "/values"),
                new Handler<ListResponse>() {
                    @Override
                    public void handle(ListResponse event) {
                        final Node node = event.getNode().getChild("set");
                        if (node == null) {
                            LOGGER.warn("set action on mirror node not found");
                            return;
                        }

                        Map<String, Node> children = event.getNode().getChildren();
                        if (children != null) {
                            for (String child : children.keySet()) {
                                LOGGER.info("Child node of values: " + child);
                            }
                        }

                        LOGGER.info("Found set path at " + node.getPath());
                        Node act = event.getNode().getChild("set");
                        if (act == null) {
                            LOGGER.warn("Set action not found on mirror");
                            return;
                        }

                        JsonObject params = new JsonObject();
                        String val = String.valueOf(RANDOM.nextDouble());
                        params.putString("value", val);
                        InvokeRequest req = new InvokeRequest(act.getPath(), params);
                        link.getRequester().invoke(req, new Handler<InvokeResponse>() {
                            @Override
                            public void handle(InvokeResponse event) {
                                LOGGER.info("Invocation successful");
                            }
                        });
                    }
                });
    }

    @Override
    public void onResponderConnected(DSLink link) {
        LOGGER.info("Responder link added");
        NodeBuilder builder = link.getNodeManager().createRootNode("values");
        final Node node = builder.build();

        builder = node.createChild("string");
        builder.setValue(new Value("Hello world"));
        builder.build();

        builder = node.createChild("mirror");
        builder.setValue(new Value(""));
        final Node mirror = builder.build();
        LOGGER.info("Old mirror value: {}", mirror.getValue().toString());

        builder = node.createChild("set");
        builder.setAction(new Action(Permission.READ, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                JsonObject params = event.getJsonIn().getObject("params");
                if (params == null) {
                    throw new NullPointerException("params");
                }

                String val = params.getString("value");
                Value value = new Value(val);

                // Set value in requester cache
                mirror.setValue(value);

                // Set value in responder cache
                event.getNode().getParent().getChild("mirror").setValue(value);

                LOGGER.info("New mirror value: " + val);
            }
        }).addParameter(new Parameter("value", ValueType.STRING)));
        builder.build();
    }

    public static void main(String[] args) {
        DSLinkFactory.startDual("dual", args, new Main());
    }
}
