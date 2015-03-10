package org.dsa.iot.dslink.client;

import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.ConnectionType;

/**
 * Argument manager designed for generating DSLinks.
 * 
 * @author Samuel Grenier
 */
public class ArgManager {

    @NonNull
    private final String[] args;

    private Map<String, String> parsedArgs = null;
    private boolean parsed = false;

    public ArgManager(String[] args) {
        this.args = args.clone();
    }

    public void parse() {
        // TODO: flexible parsing
        if (parsed) {
            return;
        }
        parsedArgs = new HashMap<>();
        if (args.length > 0 && "-url".equals(args[0])) {
            if (args.length > 1) {
                parsedArgs.put("url", args[1]);
            } else {
                throw new RuntimeException("Missing URL parameter");
            }
        }
    }

    public String getArgument(String name, String def) {
        String ret = parsedArgs.get(name);
        return ret != null ? ret : def;
    }

    /**
     * Generates Responder DSLink
     * 
     * @param args
     *            command line arguments
     * @param dsId
     *            name of {@link DSLink}
     * @return
     */
    public static DSLink generateResponder(String[] args, String dsId) {
        return generate(args, dsId, false, true);
    }

    /**
     * Generates Requester DSLink
     * 
     * @param args
     *            command line arguments
     * @param dsId
     *            name of {@link DSLink}
     * @return
     */
    public static DSLink generateRequester(String[] args, String dsId) {
        return generate(args, dsId, true, false);
    }

    private static DSLink generate(String[] args, String dsId,
            boolean isRequester, boolean isResponder) {
        val manager = new ArgManager(args);
        manager.parse();

        val bus = EventBusFactory.create();
        val url = manager.getArgument("url", "http://localhost:8080/conn");
        val type = ConnectionType.WS;
        val factory = DSLinkFactory.create();
        return factory.generate(bus, url, type, dsId, isRequester, isResponder);
    }
}
