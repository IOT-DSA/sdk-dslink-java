package org.dsa.iot.dual;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dual.requester.Requester;
import org.dsa.iot.dual.responder.Responder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Override
    public boolean isRequester() {
        return true;
    }

    @Override
    public boolean isResponder() {
        return true;
    }

    @Override
    public void onResponderInitialized(DSLink link) {
        Responder.init(link);
        LOGGER.info("Responder initialized");
    }

    @Override
    public void onRequesterConnected(final DSLink link) {
        Requester.init(link);
        LOGGER.info("Requester initialized");
    }

    public static void main(String[] args) {
        DSLinkFactory.start(args, new Main());
    }
}
