package org.dsa.iot.dslink.util.json;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
        this(Json.decodeList(content));
    }

    public JsonArray(List list) {
        if (list == null) {
            throw new NullPointerException("list");
        }
        this.list = list;
    }

    public String encode() {
        return Json.encode(this);
    }

    @SuppressWarnings("unused")
    public String encodePrettily() {
        return Json.encodePrettily(this);
    }

    public <T> T remove(int index) {
        return (T) Json.update(list.remove(index));
    }

    public <T> T get(int index) {
        return (T) Json.update(list.get(index));
    }

    public JsonArray add(Object value) {
        value = Json.checkAndUpdate(value);
        list.add(value);
        return this;
    }

    public int size() {
        return list.size();
    }

    @SuppressWarnings("unused")
    public void mergeIn(JsonArray other) {
        list.addAll(other.list);
    }

    public List<Object> getList() {
        return Collections.unmodifiableList(list);
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
