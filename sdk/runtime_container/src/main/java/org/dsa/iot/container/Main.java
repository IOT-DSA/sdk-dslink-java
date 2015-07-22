package org.dsa.iot.container;

import org.dsa.iot.container.manager.DSLinkManager;
import org.dsa.iot.container.manager.StdinHandler;
import org.dsa.iot.container.security.SecMan;
import java.nio.file.Paths;

/**
 * @author Samuel Grenier
 */
public class Main {

    public static void main(String[] args) {
        Args parsed = Args.parse(args);
        if (parsed == null) {
            return;
        }

        // Set the security manager
        System.setSecurityManager(new SecMan());

        DSLinkManager manager = new DSLinkManager();
        String dslinksFolder = parsed.getDslinksFolder();
        if (dslinksFolder != null) {
            String brokerUrl = parsed.getBrokerUrl();
            manager.loadDirectory(Paths.get(dslinksFolder), brokerUrl);
        } else {
            StdinHandler handler = new StdinHandler(manager);
            handler.start();
        }
    }
}
