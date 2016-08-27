package org.dsa.iot.dslink.serializer;

import org.dsa.iot.dslink.node.*;
import org.dsa.iot.dslink.provider.*;
import org.dsa.iot.dslink.util.*;
import org.dsa.iot.dslink.util.json.*;
import org.slf4j.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

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

    private final AtomicBoolean changed = new AtomicBoolean(false);

    /**
     * Handles serialization based on the file path.
     *
     * @param file    Path that holds the data
     * @param manager Manager to deserialize/serialize
     */
    public SerializationManager(File file, NodeManager manager) {
        this.file = file;
        this.backup = new File(file.getPath() + ".bak");
        this.deserializer = new Deserializer(manager);
        this.serializer = new Serializer(manager);
    }

    public void markChanged() {
        changed.set(true);
    }

    public void markChangedOverride(boolean bool) {
        changed.set(bool);
    }

    public synchronized void start() {
        stop();
        future = LoopProvider.getProvider().schedulePeriodic(new Runnable() {
            @Override
            public void run() {
                boolean c = changed.getAndSet(false);
                if (c) {
                    serialize();
                }
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
        try {
            JsonObject json = serializer.serialize();
            //Save the config db to a temp file.  If we can't do that, then we don't
            //want to do anything else.
            File tmp = new File(file.getParent(), file.getName() + ".tmp");
            if (tmp.exists()) {
                if (!tmp.delete()) {
                    throw new IOException("Could not delete " + tmp.getName());
                }
            }
            FileUtils.write(tmp, json.encodePrettily());
            if (!tmp.exists()) {
                throw new IOException(
                        tmp.getName() + " weirdly did not exist after writing to it");
            }
            if (tmp.length() == 0) {
                throw new IOException(tmp.getName() + " serialized to a file size of 0");
            }
            //Now backup the last version
            if (file.exists()) {
                if (backup.exists()) {
                    if (!backup.delete()) {
                        throw new IOException(
                                "Could not delete old " + backup.getName());
                    }
                }
                LOGGER.debug("Making backup");
                if (!file.renameTo(backup)) {
                    FileUtils.copy(file, backup); //try really hard
                }
                if (!backup.exists()) {
                    throw new IllegalStateException(
                            "Unable to make backup " + backup.getName());
                }
                if (file.exists()) {
                    if (!file.delete()) {
                        throw new IOException("Could not delete old " + file.getName());
                    }
                }
            }
            //Move the new tmp database into position.
            if (!tmp.renameTo(file)) {
                FileUtils.copy(tmp, file); //try really hard
            }
            if (!file.exists()) {
                //This will keep the tmp file in place, although the next serialization
                //attempt will probably delete it
                throw new IOException(
                        "Failed to move " + tmp.getName() + " to " + file.getName());
            }
            if (tmp.exists()) {
                tmp.delete();
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Wrote serialized data: {}", json);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration database", e);
        }
    }

    /**
     * Deserializes the data into the node manager based on the path.
     *
     * @throws Exception An error has occurred deserializing the nodes.
     */
    public void deserialize() throws Exception {
        if (file.exists()) {
            try {
                handle(FileUtils.readAllBytes(file));
                LOGGER.debug("Restored " + file.getName());
                return;
            } catch (Exception x) {
                LOGGER.error("Could not deserialize " + file.getName(), x);
            }
        }
        //There was a problem with the primary db.
        if (backup.exists()) {
            try {
                handle(FileUtils.readAllBytes(backup));
                LOGGER.warn("Restored backup " + backup.getName());
                if (file.exists()) {
                    //Try delete the primary db so it won't overwrite the
                    //good backup during the next serialization
                    file.delete();
                }
                return;
            } catch (Exception x) {
                LOGGER.error("Could not delete " + file.getName(), x);
            }
        }
        //We've got nothing to lose, try the tmp serialization file
        File tmp = new File(file.getParent(), file.getName() + ".tmp");
        if (tmp.exists()) {
            try {
                handle(FileUtils.readAllBytes(file));
                LOGGER.warn("Restored " + tmp.getName());
                return;
            } catch (Exception x) {
                LOGGER.error("Could not deserialize " + tmp.getName(), x);
            }
        }
        LOGGER.warn("Unable to deserialize a configuration database");
    }

    private void handle(byte[] bytes) throws Exception {
        String in = new String(bytes, "UTF-8");
        JsonObject obj = new JsonObject(in);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Read serialized data: " + obj);
        }
        deserializer.deserialize(obj);
    }

    static {
        LOGGER = LoggerFactory.getLogger(SerializationManager.class);
    }
}
