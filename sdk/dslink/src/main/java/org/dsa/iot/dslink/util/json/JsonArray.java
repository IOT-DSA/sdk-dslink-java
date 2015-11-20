package org.dsa.iot.dslink.util.json;

import io.netty.util.CharsetUtil;

import java.util.*;

/**
 * @author Samuel Grenier
 */
@SuppressWarnings("unchecked")
public class JsonArray implements Iterable<Object> {

    private final List<Object> list;

    public JsonArray() {
        this(new LinkedList<>());
    }

    public JsonArray(String content) {
        this(EncodingFormat.JSON, content.getBytes(CharsetUtil.UTF_8));
    }

    public JsonArray(EncodingFormat format, byte[] content) {
        this(Json.decodeList(format, content));
    }

    public JsonArray(List list) {
        if (list == null) {
            throw new NullPointerException("list");
        }
        this.list = list;
    }

    @SuppressWarnings("unused")
    public byte[] encode() {
        return encode(EncodingFormat.JSON);
    }

    public byte[] encode(EncodingFormat format) {
        return Json.encode(format, this);
    }

    @SuppressWarnings("unused")
    public byte[] encodePrettily() {
        return encodePrettily(EncodingFormat.JSON);
    }

    public byte[] encodePrettily(EncodingFormat format) {
        return Json.encodePrettily(format, this);
    }

    public <T> T remove(int index) {
        return (T) Json.update(list.remove(index));
    }

    public <T> T get(int index) {
        return (T) Json.update(list.get(index));
    }

    public <T> T set(int index, Object value) {
        value = Json.checkAndUpdate(value);
        return (T) list.set(index, value);
    }

    public JsonArray add(Object value) {
        value = Json.checkAndUpdate(value);
        list.add(value);
        return this;
    }

    public JsonArray add(int pos, Object value) {
        value = Json.checkAndUpdate(value);
        list.add(pos, value);
        return this;
    }

    public JsonArray addAll(int pos, List<Object> values) {
        if (values != null) {
            ListIterator it = values.listIterator();
            while (it.hasNext()) {
                Object o = it.next();
                it.set(Json.checkAndUpdate(o));
            }
            list.addAll(pos, values);
        }
        return this;
    }

    public int size() {
        return list.size();
    }

    public void mergeIn(JsonArray other) {
        list.addAll(other.list);
    }

    public List<Object> getList() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public String toString() {
        return new String(encode(EncodingFormat.JSON), CharsetUtil.UTF_8);
    }

    @Override
    public Iterator<Object> iterator() {
        return new JsonIterator(list.iterator());
    }

    private static class JsonIterator implements Iterator<Object> {

        private final Iterator<Object> it;

        public JsonIterator(Iterator<Object> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Object next() {
            return Json.update(it.next());
        }
    }
}
