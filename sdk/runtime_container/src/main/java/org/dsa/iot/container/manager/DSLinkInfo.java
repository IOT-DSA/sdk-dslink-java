package org.dsa.iot.container.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dsa.iot.container.wrapper.DSLink;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Samuel Grenier
 */
public class DSLinkInfo {

    private final String name;
    private final String logLevel;
    private final String handlerClass;

    public DSLinkInfo(String name, String logLevel, String handlerClass) {
        if (name == null) {
            throw new NullPointerException("name");
        } else if (logLevel == null) {
            throw new NullPointerException("logLevel");
        } else if (handlerClass == null) {
            throw new NullPointerException("handlerClass");
        }
        this.name = name;
        this.logLevel = logLevel;
        this.handlerClass = handlerClass;
    }

    public static DSLink load(Path root) {
        Path libFolder = root.resolve("lib");
        if (!Files.isDirectory(libFolder)) {
            String err = "Missing `lib` folder for ";
            err += root.toString();
            System.err.println(err);
            return null;
        }

        Path dslinkJson = root.resolve("dslink.json");
        if (!Files.isRegularFile(dslinkJson)) {
            String err = "Missing dslink.json for ";
            err += root.toString();
            System.err.println(err);
            return;
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
                return;
            }

            name = config.path("name").get("default").asText();
            logLevel = config.path("log").get("default").asText();
            handler = config.path("handler_class").get("default").asText();
        }

        final ClassLoader loader;
        {
            JarWalker walker = new JarWalker();
            Files.walkFileTree(libFolder, walker);

            URL[] urls = walker.getUrls();
            loader = new URLClassLoader(urls);
        }
    }

}
