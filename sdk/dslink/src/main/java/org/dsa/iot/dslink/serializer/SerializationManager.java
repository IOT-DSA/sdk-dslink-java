package org.dsa.iot.dslink.serializer;

import org.dsa.iot.dslink.node.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Handles automatic serialization and deserialization.
 *
 * @author Samuel Grenier
 */
public class SerializationManager {

    private static final Logger LOGGER;

    private final Path path;
    private final Path backup;

    private final Deserializer deserializer;
    private final Serializer serializer;

    private final ScheduledThreadPoolExecutor stpe;
    private ScheduledFuture<?> future;

    /**
     * Handles serialization based on the file path.
     *
     * @param path Path that holds the data
     * @param manager Manager to deserialize/serialize
     */
    public SerializationManager(Path path, NodeManager manager) {
        this.path = path;
        this.backup = Paths.get(path.toString() + ".bak");
        this.deserializer = new Deserializer(manager);
        this.serializer = new Serializer(manager);
        this.stpe = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
    }

    public synchronized void start() {
        stop();
        future = stpe.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                serialize();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    /**
     * Serializes the data from the node manager into the file based on the
     * path. Manually calling this is redundant as a timer will automatically
     * handle serialization.
     */
    public void serialize() {
        JsonObject output = serializer.serialize();
        try {
            if (Files.exists(path)) {
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(path);
            }
            String out = output.encodePrettily();
            byte[] bytes = out.getBytes("UTF-8");
            Files.write(path, bytes);
            Files.delete(backup);
            LOGGER.debug("Written serialized data: " + out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes the data into the node manager based on the path.
     */
    public void deserialize() {
        try {
            byte[] bytes = null;
            if (Files.exists(path)) {
                bytes = Files.readAllBytes(path);
            } else if (Files.exists(backup)) {
                bytes = Files.readAllBytes(backup);
                Files.copy(backup, path);
            }
            Files.deleteIfExists(backup);

            if (bytes != null) {
                String in = new String(bytes, "UTF-8");
                LOGGER.debug("Read serialized data: " + in);
                JsonObject obj = new JsonObject(in);
                deserializer.deserialize(obj);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        LOGGER = LoggerFactory.getLogger(SerializationManager.class);
    }
}
