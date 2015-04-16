package org.dsa.iot.dual.requester;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.requests.SetRequest;
import org.dsa.iot.dslink.methods.responses.SetResponse;
import org.dsa.iot.dslink.node.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Requester extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Requester.class);

    /**
     * Initializes the requester link.
     *
     * @param link Requester link to initialize.
     */
    public static void init(DSLink link) {
        setNodeValue(link);
    }

    /**
     *
     * @param link Requester link used to communicate to the endpoint.
     * @see org.dsa.iot.dual.responder.Responder#initSettableNode
     */
    private static void setNodeValue(DSLink link) {
        Value value = new Value("Hello world!");
        SetRequest request = new SetRequest("/conns/dual/values/settable", value);
        link.getRequester().set(request, new Handler<SetResponse>() {
            @Override
            public void handle(SetResponse event) {
                LOGGER.info("Successfully set the new value on the responder");
            }
        });
    }
}
