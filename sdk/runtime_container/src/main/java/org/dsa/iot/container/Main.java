package org.dsa.iot.container;

import org.dsa.iot.container.manager.DSLinkManager;

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

        String dslinksFolder = parsed.getDslinksFolder();
        if (dslinksFolder != null) {
            String brokerUrl = parsed.getBrokerUrl();
            DSLinkManager manager = new DSLinkManager();
            manager.loadDirectory(Paths.get(dslinksFolder), brokerUrl);
        } else {
            // TODO: handle stdin from the dslink manager
        }
    }
}
