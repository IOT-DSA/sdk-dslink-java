package org.dsa.iot.dual;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dual.requester.Requester;
import org.dsa.iot.dual.responder.Responder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private CountDownLatch latch;

    @Override
    public void preInit() {
        // Latch is used to ensure responder is initialized first
        latch = new CountDownLatch(1);
    }

    @Override
    public void onRequesterConnected(final DSLink link) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Requester.init(link);
        LOGGER.info("Requester initialized");
    }

    @Override
    public void onResponderInitialized(DSLink link) {
        Responder.init(link);
        LOGGER.info("Responder initialized");
        latch.countDown();
    }

    public static void main(String[] args) {
        DSLinkFactory.startDual("dual", args, new Main());
    }
}
