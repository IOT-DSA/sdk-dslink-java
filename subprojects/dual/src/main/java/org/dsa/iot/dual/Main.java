package org.dsa.iot.dual;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public void onRequesterConnected(DSLink link) {
        LOGGER.info("Requester link added");
        link.getRequester().list(new ListRequest("/"),
                new Handler<ListResponse>() {
            @Override
            public void handle(ListResponse event) {
                LOGGER.info("Request on root node complete");
            }
        });
    }

    public void onResponderConnected(DSLink link) {
        LOGGER.info("Responder link added");
    }

    public static void main(String[] args) {
        DSLinkFactory.startDual("dual", args, new Main());
    }
}
