package org.dsa.iot.container.manager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class DSLinkManager {

    private final Map<String, DSLinkHandler> links = new HashMap<>();

    public synchronized void loadDirectory(Path path, String brokerUrl) {
        if (!Files.isDirectory(path)) {
            System.err.println(path.toString() + " is not a directory");
            return;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
            for (Path dslinkRoot : ds) {
                if (!Files.isDirectory(dslinkRoot)) {
                    continue;
                }
                DSLinkInfo info = DSLinkInfo.load(dslinkRoot, brokerUrl);
                if (info == null) {
                    continue;
                }
                try {
                    start(dslinkRoot, info);
                } catch (Exception e) {
                    System.err.println("Failed to start: " + info.getName());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void start(Path path, DSLinkInfo info) {
        String name = path.toString();
        DSLinkHandler link = links.get(name);
        if (link == null) {
            link = new DSLinkHandler(info);
            links.put(name, link);
        }
        try {
            link.start();
        } catch (IOException e) {
            links.remove(name);
            throw new RuntimeException(e);
        }
    }

    public synchronized void stop(Path path) {
        String name = path.toString();
        DSLinkHandler link = links.remove(name);
        if (link != null) {
            link.stop();
        }
    }
}
