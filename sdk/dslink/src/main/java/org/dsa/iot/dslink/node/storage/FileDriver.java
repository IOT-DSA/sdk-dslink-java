package org.dsa.iot.dslink.node.storage;

import io.netty.util.CharsetUtil;
import org.dsa.iot.dslink.node.SubscriptionManager.Subscription;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.FileUtils;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Samuel Grenier
 */
public class FileDriver implements StorageDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDriver.class);
    private final File storageDir = new File("storage");

    private final Map<String, Queue<Value>> updatesCache = new HashMap<>();
    private final Map<String, Value> updateCache = new HashMap<>();

    @Override
    public void read(Map<String, Subscription> map) {
        if (!storageDir.isDirectory()) {
            return;
        }
        File[] files = storageDir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            try {
                if (f == null || !f.getName().startsWith("%2F")) {
                    continue;
                }
                String s = new String(FileUtils.readAllBytes(f), CharsetUtil.UTF_8);
                JsonObject obj = new JsonObject(s);

                int qos = obj.get("qos");
                String path = StringUtils.decodeName(f.getName());
                Subscription sub = new Subscription(path, -1, qos);
                map.put(path, sub);
                if (qos == 2) {
                    String ts = obj.get("ts");
                    Value val = ValueUtils.toValue(obj.get("value"), ts);
                    store(sub, val);
                    updateCache.put(path, val);
                } else if (qos == 3) {
                    JsonArray jsonQueue = obj.get("queue");
                    if (jsonQueue == null) {
                        continue;
                    }
                    Queue<Value> queue = updatesCache.get(path);
                    if (queue == null) {
                        synchronized (this) {
                            queue = updatesCache.get(sub.path());
                            if (queue == null) {
                                queue = new LinkedBlockingQueue<>();
                                updatesCache.put(sub.path(), queue);
                            }
                        }
                    }
                    for (Object o : jsonQueue) {
                        JsonArray array = (JsonArray) o;
                        String ts = array.get(1);
                        Value v = ValueUtils.toValue(array.get(0), ts);
                        queue.add(v);
                    }
                }
            } catch (Exception e) {
                String path = f.getName();
                String err = "Failed to handle QoS subscription data: {}\n{}";
                LOGGER.warn(err, path, e);
            }
        }
        
    }

    @Override
    public void store(Subscription sub, Value value) {
        JsonObject obj = null;
        if (sub.qos() == 2) {
            updateCache.put(sub.path(), value);
            if (!storageDir.exists() && storageDir.mkdir()) {
                String full = storageDir.getAbsolutePath();
                LOGGER.info("Created storage directory at {}", full);
            }
            obj = new JsonObject();
            obj.put("qos", 2);
            if (value != null) {
                obj.put("value", value);
                obj.put("ts", value.getTimeStamp());
            }
        } else if (sub.qos() == 3) {
            if (value == null) {
                return;
            }
            Queue<Value> cache = updatesCache.get(sub.path());
            if (cache == null) {
                synchronized (this) {
                    cache = updatesCache.get(sub.path());
                    if (cache == null) {
                        cache = new LinkedBlockingQueue<>();
                        updatesCache.put(sub.path(), cache);
                    }
                }
            }
            cache.add(value);
            if (cache.size() > 1000) {
                cache.remove();
            }
            obj = new JsonObject();
            JsonArray queue = new JsonArray();
            obj.put("queue", queue);
            obj.put("qos", 3);
            for (Value v : cache) {
                if (v == null) {
                    queue.add(null);
                } else {
                    JsonArray array = new JsonArray();
                    array.add(v);
                    array.add(v.getTimeStamp());
                    queue.add(array);
                }
            }
        }
        if (obj != null) {
            File f = new File(storageDir, StringUtils.encodeName(sub.path()));
            try {
                FileUtils.write(f, obj.encode().getBytes(CharsetUtil.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public JsonArray getUpdates(Subscription sub) {
        Queue<Value> cache = updatesCache.remove(sub.path());
        {
            Value tmp = updateCache.remove(sub.path());
            if (tmp != null) {
                return sub.generateUpdate(tmp);
            }
            if (cache == null || cache.isEmpty()) {
                return null;
            }
        }
        JsonArray updates = new JsonArray();
        Value val;
        while ((val = cache.poll()) != null) {
            JsonArray update = sub.generateUpdate(val);
            updates.add(update);
        }
        {
            File f = new File(storageDir, sub.path());
            if (f.exists() && !f.delete()) {
                LOGGER.warn("Failed to delete QoS data at {}", sub.path());
            }
        }
        return updates;
    }
}
