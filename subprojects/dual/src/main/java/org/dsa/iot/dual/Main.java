package org.dsa.iot.dual;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.DSLinkProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
public class Main extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * @param link The link that has completed a connection.
     */
    @SuppressWarnings("UnusedParameters")
    public void onRequesterConnected(DSLink link) {
        LOGGER.info("Requester link added");
    }

    /**
     *
     * @param link The link that has completed a connection.
     */

    @SuppressWarnings("UnusedParameters")
    public void onResponderConnected(DSLink link) {
        LOGGER.info("Responder link added");
    }

    public static void main(String[] args) {
        String name = "dual";
        DSLinkProvider p = DSLinkFactory.generate(name, args, new Main(), true, true);
        if (p != null) {
            p.start();
            p.sleep();
        }
    }
}
