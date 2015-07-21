package org.dsa.iot.container.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Grenier
 */
public class DSLinkInfo {

    private final Path root;
    private final String name;
    private final String logLevel;
    private final String handlerClass;
    private final String brokerUrl;

    public DSLinkInfo(Path path,
                      String name,
                      String logLevel,
                      String handlerClass,
                      String brokerUrl) {
        if (path == null) {
            throw new NullPointerException("path");
        } else if (name == null) {
            throw new NullPointerException("name");
        } else if (logLevel == null) {
            throw new NullPointerException("logLevel");
        } else if (handlerClass == null) {
            throw new NullPointerException("handlerClass");
        } else if (brokerUrl == null) {
            throw new NullPointerException("brokerUrl");
        }
        this.root = path;
        this.name = name;
        this.logLevel = logLevel;
        this.handlerClass = handlerClass;
        this.brokerUrl = brokerUrl;
    }

    public Path getRoot() {
        return root;
    }

    public String getName() {
        return name;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getHandlerClass() {
        return handlerClass;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public URL[] collectJars() throws IOException {
        JarWalker walker = new JarWalker();
        Files.walkFileTree(root.resolve("lib"), walker);
        return walker.getUrls();
    }

    public static DSLinkInfo load(Path root, String brokerUrl)
                                                throws IOException {
        Path dslinkJson = root.resolve("dslink.json");
        if (!Files.isRegularFile(dslinkJson)) {
            String err = "Missing dslink.json for ";
            err += root.toString();
            System.err.println(err);
            return null;
        }

        final String name;
        final String logLevel;
        final String handler;
        {
            ObjectMapper mapper = new ObjectMapper();
            byte[] readJson = Files.readAllBytes(dslinkJson);
            ObjectNode node = mapper.readValue(readJson, ObjectNode.class);
            JsonNode config = node.path("configs");
            if (config.isMissingNode()) {
                String err = "Missing configs field in dslink.json for ";
                err += root.toString();
                System.err.println(err);
                return null;
            }

            name = config.path("name").get("default").asText();
            logLevel = config.path("log").get("default").asText();
            handler = config.path("handler_class").get("default").asText();
        }

        return new DSLinkInfo(root, name, logLevel, handler, brokerUrl);
    }

    private static class JarWalker extends SimpleFileVisitor<Path> {

        private List<URL> urls = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attr) {
            if (attr.isRegularFile() && file.toString().endsWith(".jar")) {
                try {
                    urls.add(file.toUri().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        public URL[] getUrls() {
            return this.urls.toArray(new URL[this.urls.size()]);
        }
    }
}
