package org.dsa.iot.dslink.node;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.CharsetUtil;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.dsa.iot.dslink.node.SubscriptionManager.Subscription;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.FileUtils;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samuel Grenier
 */
class FileDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDriver.class);

    private final File storageDir = new File("storage");
    private SubscriptionManager subscriptionManager;
    private final Map<String, Queue<Value>> updatesCache = new HashMap<>();

    FileDriver(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    void clear(Subscription sub) {
        File f = new File(storageDir, sub.getPath());
        if (f.exists()) {
            f.delete();
        }
    }

    void restore() {
        if (!storageDir.isDirectory()) {
            return;
        }
        File[] files = storageDir.listFiles();
        if (files == null) {
            return;
        }
        Queue<Value> queue = null;
        for (File f : files) {
            try {
                if (f == null || !f.getName().startsWith("%2F")) {
                    continue;
                }
                String s = new String(FileUtils.readAllBytes(f), CharsetUtil.UTF_8);
                JsonObject obj = new JsonObject(s);
                String path = StringUtils.decodeName(f.getName());
                JsonArray jsonQueue = obj.get("queue");
                if (jsonQueue != null) {
                    queue = new LinkedList<Value>();
                    for (Object o : jsonQueue) {
                        JsonArray array = (JsonArray) o;
                        queue.add(ValueUtils.toValue(array.get(0), array.get(1).toString()));
                    }
                } else {
                   queue = null;
                }
                subscriptionManager.restore(path, queue);
            } catch (Exception e) {
                String path = f.getName();
                String err = "Failed to handle QoS subscription data: {}\n{}";
                LOGGER.warn(err, path, e);
            }
        }
    }

    void store(Subscription sub) {
        if (!(storageDir.exists() || storageDir.mkdirs())) {
            String full = storageDir.getAbsolutePath();
            LOGGER.info("Failed to create storage directory at {}", full);
        }
        Queue<Value> cache = sub.getUpdates();
        JsonObject obj = new JsonObject();
        JsonArray queue = new JsonArray(); //backward compatible structure
        obj.put("queue", queue);
        obj.put("qos", sub.getQos());
        for (Value v : cache) {
            JsonArray array = new JsonArray();
            array.add(v);
            array.add(v.getTimeStamp());
            queue.add(array);
        }
        File f = new File(storageDir, StringUtils.encodeName(sub.getPath()));
        try {
            FileUtils.write(f, obj.encode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
