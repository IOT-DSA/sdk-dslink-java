package org.dsa.iot.dslink.serializer;

import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.util.FileUtils;
import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Handles automatic serialization and deserialization.
 *
 * @author Samuel Grenier
 */
public class SerializationManager {

    private static final Logger LOGGER;

    private final File file;
    private final File backup;

    private final Deserializer deserializer;
    private final Serializer serializer;
    private ScheduledFuture<?> future;

    /**
     * Handles serialization based on the file path.
     *
     * @param file Path that holds the data
     * @param manager Manager to deserialize/serialize
     */
    public SerializationManager(File file, NodeManager manager) {
        this.file = file;
        this.backup = new File(file.getPath() + ".bak");
        this.deserializer = new Deserializer(manager);
        this.serializer = new Serializer(manager);
    }

    public synchronized void start() {
        stop();
        ScheduledThreadPoolExecutor daemon = Objects.getDaemonThreadPool();
        future = daemon.scheduleWithFixedDelay(new Runnable() {
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
        JsonObject json = serializer.serialize();
        try {
            if (file.exists()) {
                FileUtils.copy(file, backup);
                LOGGER.debug("Copying serialized data to a backup");
                if (file.delete()) {
                    LOGGER.debug("Serialized data removed");
                }
            }
            String out = json.encodePrettily();
            byte[] bytes = out.getBytes("UTF-8");
            FileUtils.write(file, bytes);

            if (backup.delete()) {
                LOGGER.debug("Backup data removed");
            }

            if (LOGGER.isDebugEnabled()) {
                out = json.encode();
                LOGGER.debug("Wrote serialized data: " + out);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save serialized data", e);
        }
    }

    /**
     * Deserializes the data into the node manager based on the path.
     */
    public void deserialize() {
        try {
            byte[] bytes = null;
            if (file.exists()) {
                bytes = FileUtils.readAllBytes(file);
            } else if (backup.exists()) {
                bytes = FileUtils.readAllBytes(backup);
            }
            if (backup.delete()) {
                if (bytes != null) {
                    FileUtils.write(file, bytes);
                }
                LOGGER.debug("Moved backup data to regular data for deserialization");
            }

            if (bytes != null) {
                String in = new String(bytes, "UTF-8");
                JsonObject obj = new JsonObject(in);
                if (LOGGER.isDebugEnabled()) {
                    in = obj.encode();
                    LOGGER.debug("Read serialized data: " + in);
                }
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
