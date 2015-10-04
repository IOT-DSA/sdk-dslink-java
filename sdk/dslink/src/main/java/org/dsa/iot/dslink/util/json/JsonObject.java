package org.dsa.iot.dslink.util.json;

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
        this(Json.decodeMap(json));
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

    public String encode() {
        return Json.encode(this);
    }

    @SuppressWarnings("unused")
    public String encodePrettily() {
        return Json.encodePrettily(this);
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
        map.putAll(other.map);
    }

    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return encode();
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
