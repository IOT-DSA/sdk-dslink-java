package org.dsa.iot.dslink.client;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.dsa.iot.core.event.EventBusFactory;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.connection.ConnectionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Argument manager designed for generating DSLinks.
 * @author Samuel Grenier
 */
public class ArgManager {

    @NonNull private final String[] args;

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

    public static DSLink generate(String[] args, String dsId) {
        return generate(args, dsId, false, true);
    }

    public static DSLink generate(String[] args,
                                  String dsId,
                                  boolean isRequester,
                                  boolean isResponder) {
        val manager = new ArgManager(args);
        manager.parse();

        val bus = EventBusFactory.create();
        val url = manager.getArgument("url", "http://localhost:8081/conn");
        val type = ConnectionType.WS;
        val fact = DSLinkFactory.create();
        return fact.generate(bus, url, type, dsId, isRequester, isResponder);
    }
}
