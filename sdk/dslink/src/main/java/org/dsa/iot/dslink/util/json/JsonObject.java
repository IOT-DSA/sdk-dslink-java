package org.dsa.iot.dslink.util.json;

import io.netty.util.CharsetUtil;
import org.dsa.iot.dslink.connection.TransportFormat;

import java.util.*;

/**
 * @author Samuel Grenier
 */
@SuppressWarnings("unchecked")
public class JsonObject implements Iterable<Map.Entry<String, Object>> {

    private final Map<String, Object> map;

    public JsonObject() {
        this(new LinkedHashMap<String, Object>());
    }

    public JsonObject(String json) {
        this(TransportFormat.JSON, json.getBytes(CharsetUtil.UTF_8));
    }

    public JsonObject(TransportFormat format, byte[] json) {
        this(Json.decodeMap(format, json));
    }

    public JsonObject(Map<String, Object> map) {
        if (map == null) {
            throw new NullPointerException("map");
        }
        this.map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public byte[] encode() {
        return encode(TransportFormat.JSON);
    }

    public byte[] encode(TransportFormat format) {
        return Json.encode(format, this);
    }

    public byte[] encodePrettily() {
        return encodePrettily(TransportFormat.JSON);
    }

    public byte[] encodePrettily(TransportFormat format) {
        return Json.encodePrettily(format, this);
    }

    public boolean contains(String key) {
        return map.containsKey(key);
    }

    public <T> T remove(String key) {
        return (T) Json.update(map.remove(key));
    }

    public <T> T get(String key) {
        return get(key, null);
    }


    public <T> T get(String key, T def) {
        T t = (T) Json.update(map.get(key));
        return t != null ? t : def;
    }

    public JsonObject put(String key, Object value) {
        Objects.requireNonNull(key);
        value = Json.checkAndUpdate(value);
        map.put(key, value);
        return this;
    }

    public int size() {
        return map.size();
    }

    public void mergeIn(JsonObject other) {
        mergeIn(other, false);
    }

    public void mergeIn(JsonObject other, boolean deep) {
        if (deep) {
            for (Map.Entry<String, Object> entry : other.map.entrySet()) {
                String name = entry.getKey();
                Object instance = entry.getValue();
                if (instance instanceof JsonArray) {
                    Object thisInst = map.get(name);
                    if (thisInst instanceof JsonArray) {
                        ((JsonArray) thisInst).mergeIn((JsonArray) instance);
                    }
                } else if (instance instanceof JsonObject) {
                    Object thisInst = map.get(name);
                    if (thisInst instanceof JsonObject) {
                        JsonObject jsonObject = (JsonObject) thisInst;
                        jsonObject.mergeIn((JsonObject) instance, true);
                    }
                } else {
                    map.put(name, instance);
                }
            }
            return;
        }
        map.putAll(other.map);
    }

    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return new String(encode(TransportFormat.JSON), CharsetUtil.UTF_8);
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return new JsonIterator(map.entrySet().iterator());
    }

    private static class JsonIterator
            implements Iterator<Map.Entry<String, Object>> {

        private final Iterator<Map.Entry<String, Object>> it;

        public JsonIterator(Iterator<Map.Entry<String, Object>> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Map.Entry<String, Object> next() {
            Map.Entry<String, Object> entry = it.next();
            entry.setValue(Json.update(entry.getValue()));
            return entry;
        }
    }
}
