package org.dsa.iot.container.manager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class DSLinkManager {

    private final Map<String, DSLinkHandler> links = new HashMap<>();
    private final List<String> loadedNatives = new ArrayList<>();
    private final Path temporaryDir;

    public DSLinkManager() {
        temporaryDir = Paths.get("native-tmp");
        try {
            if (Files.exists(temporaryDir)
                    && !Files.isDirectory(temporaryDir)) {
                if (!Files.deleteIfExists(temporaryDir)) {
                    String err = "Failed to delete: ";
                    throw new IOException(err + temporaryDir.toString());
                }
                Files.createDirectory(temporaryDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
                try {
                    load(dslinkRoot, brokerUrl);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void load(Path path, String brokerUrl) throws Exception {
        DSLinkInfo info = DSLinkInfo.load(path, brokerUrl);
        if (info == null) {
            return;
        }
        try {
            start(path, info);
        } catch (Exception e) {
            System.err.println("Failed to start: " + info.getName());
        }
    }

    public synchronized void start(Path path, DSLinkInfo info) {
        String name = path.toString();
        DSLinkHandler link = links.get(name);
        if (link == null) {
            link = new DSLinkHandler(this, info);
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

    public synchronized void loadNative(URL url) {
        Path file = urlToPath(url);
        Path fileName = file.getFileName();
        if (fileName == null) {
            return;
        }
        final String fullName = fileName.toString();
        String shortName = fullName;
        { // Strip out version information
            int index = shortName.lastIndexOf("-");
            if (index > -1) {
                shortName = shortName.substring(0, index);
            }
            if (shortName.endsWith(".jar")) {
                index = shortName.lastIndexOf(".jar");
                index = shortName.length() - index;
                shortName = shortName.substring(0, index);
            }
        }
        { // Tell the system it is loaded
            if (loadedNatives.contains(shortName)) {
                String msg = "";
                msg += "Prevented native library from loading: ";
                msg += shortName + " (" + fullName + ")";
                System.err.println(msg);
                return;
            }
            String msg = "";
            msg += "Loaded native library: ";
            msg += shortName + " (" + fullName + ")";
            System.err.println(msg);
            loadedNatives.add(shortName);
        }
        try { // Copy the file
            Path path = temporaryDir;
            path = Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING);
            url = path.toUri().toURL();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        { // Add to the class path
            final Thread t = Thread.currentThread();
            final ClassLoader l = t.getContextClassLoader();
            final URLClassLoader loader = (URLClassLoader) l;
            try {
                Class<?> clazz = URLClassLoader.class;
                Method m = clazz.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);
                m.invoke(loader, url);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Path urlToPath(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
