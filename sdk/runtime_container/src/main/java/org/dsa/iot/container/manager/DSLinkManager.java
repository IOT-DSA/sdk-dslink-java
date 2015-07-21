package org.dsa.iot.container.manager;

import org.dsa.iot.container.wrapper.DSLink;

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

    private final Map<String, DSLink> links = new HashMap<>();

    public void loadDirectory(Path path) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
            for (Path dslinkRoot : ds) {

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(Path path, Map<String, Object> configs) {
        String name = path.toString();
        DSLink link = links.get(name);
        if (link == null) {
            link = new DSLink();
            links.put(name, link);
        }
        link.start();
    }

    public void stop(Path path) {
        String name = path.toString();
        DSLink link = links.get(name);
        if (link != null) {
            link.stop();
        }
    }
}
